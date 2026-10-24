package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.OrderFilter;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CreateOrderCommand;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.OrderDetails;
import com.escapa.backend.application.model.OrderSummary;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Order;
import com.escapa.backend.domain.order.CourseAlreadyActiveException;
import com.escapa.backend.domain.order.OrderItemType;
import com.escapa.backend.domain.order.OrderStatus;
import com.escapa.backend.domain.order.OrderType;
import com.escapa.backend.domain.order.OrderValidationException;
import com.escapa.backend.domain.order.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderUseCaseTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(14, 30).atZone(ZONE).toInstant(), ZONE);
    private static final BigDecimal SEAT_PRICE = new BigDecimal("79.00");

    private final UUID buyerId = UUID.randomUUID();

    private InMemoryCourseRepositoryPort courses;
    private FakeOrders orders;
    private FakeAccess access;
    private CreateOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        courses = new InMemoryCourseRepositoryPort();
        orders = new FakeOrders();
        access = new FakeAccess();
        useCase = new CreateOrderUseCase(orders, courses, access, SEAT_PRICE, CLOCK);
    }

    private Course givenCourse(CourseStatus status, Double price, Integer deadline) {
        final Course course = new Course();
        course.setTitle("Marketing Digital para Hospitalidade");
        course.setStatus(status);
        course.setPrice(price);
        course.setDeadline(deadline);
        return courses.save(course);
    }

    private CreateOrderCommand courseOrder(String role, UUID courseId, PaymentMethod method, Integer installments) {
        return new CreateOrderCommand(buyerId, role, OrderItemType.COURSE, courseId, null, method, installments);
    }

    private CreateOrderCommand seatsOrder(String role, Integer quantity) {
        return new CreateOrderCommand(buyerId, role, OrderItemType.SEATS, null, quantity, PaymentMethod.CARD, 1);
    }

    private void assertNothingWasRegistered() {
        assertTrue(orders.saved.isEmpty());
        assertTrue(access.studentEnrollments.isEmpty());
        assertTrue(access.companyCourses.isEmpty());
        assertTrue(access.seatsAdded.isEmpty());
    }

    @Test
    void shouldApproveTheCourseOrderOfAStudentAndEnrollThemFromTodayUntilTheDeadline() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 297.0, 365);

        final Order order = useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 3));

        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertEquals(OrderType.INDIVIDUAL, order.getType());
        assertEquals(OrderItemType.COURSE, order.getItemType());
        assertEquals(buyerId, order.getBuyerUserId());
        assertNull(order.getCompanyId());
        assertEquals(course.getId(), order.getCourseId());
        assertNull(order.getSeatsQuantity());
        assertEquals(new BigDecimal("297.00"), order.getAmount());
        assertEquals(PaymentMethod.CARD, order.getPaymentMethod());
        assertEquals(3, order.getInstallments());
        assertEquals(LocalDateTime.of(2026, 10, 10, 14, 30), order.getCreatedAt());
        assertEquals(1, order.getStatusHistory().size());
        assertEquals(OrderStatus.APPROVED, order.getStatusHistory().get(0).status());
        assertEquals(new Enrollment(TODAY, TODAY.plusDays(365)), access.studentEnrollments.get(key(buyerId, course)));
        assertTrue(access.companyCourses.isEmpty());
        assertTrue(access.seatsAdded.isEmpty());
    }

    @Test
    void shouldEnrollWithoutAnEndDateWhenTheCourseHasNoDeadline() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, null);

        useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1));

        assertEquals(new Enrollment(TODAY, null), access.studentEnrollments.get(key(buyerId, course)));
    }

    @Test
    void shouldTakeTheAmountFromTheCoursePriceWithTwoDecimals() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 127.5, 30);

        final Order order = useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1));

        assertEquals(new BigDecimal("127.50"), order.getAmount());
    }

    @Test
    void shouldChargeNothingForACourseWithoutPrice() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, null, 30);

        final Order order = useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1));

        assertEquals(new BigDecimal("0.00"), order.getAmount());
    }

    @Test
    void shouldUseOneInstallmentWhenNoneIsInformed() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);

        final Order order = useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, null));

        assertEquals(1, order.getInstallments());
    }

    @Test
    void shouldRegisterTheCourseOfACompanyFromTodayWithoutAnEndDate() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 297.0, 365);

        final Order order = useCase.execute(courseOrder("COMPANY", course.getId(), PaymentMethod.CARD, 1));

        assertEquals(OrderType.CORPORATE, order.getType());
        assertEquals(buyerId, order.getCompanyId());
        assertEquals(course.getId(), order.getCourseId());
        assertEquals(new BigDecimal("297.00"), order.getAmount());
        assertEquals(new Enrollment(TODAY, null), access.companyCourses.get(key(buyerId, course)));
        assertTrue(access.studentEnrollments.isEmpty());
        assertTrue(access.seatsAdded.isEmpty());
    }

    @Test
    void shouldAddTheSeatsOfACompanyWithoutReleasingAnyCourse() {
        final Order order = useCase.execute(seatsOrder("COMPANY", 10));

        assertEquals(OrderType.CORPORATE, order.getType());
        assertEquals(OrderItemType.SEATS, order.getItemType());
        assertEquals(buyerId, order.getCompanyId());
        assertEquals(10, order.getSeatsQuantity());
        assertNull(order.getCourseId());
        assertEquals(new BigDecimal("790.00"), order.getAmount());
        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertEquals(10, access.seatsAdded.get(buyerId));
        assertTrue(access.studentEnrollments.isEmpty());
        assertTrue(access.companyCourses.isEmpty());
    }

    @Test
    void shouldMultiplyTheConfiguredSeatPriceByTheQuantity() {
        final CreateOrderUseCase custom = new CreateOrderUseCase(
                orders, courses, access, new BigDecimal("12.345"), CLOCK);

        final Order order = custom.execute(seatsOrder("COMPANY", 3));

        assertEquals(new BigDecimal("37.04"), order.getAmount());
    }

    @Test
    void shouldRefuseSeatsBoughtByAStudent() {
        assertThrows(OrderValidationException.class, () -> useCase.execute(seatsOrder("STUDENT", 10)));

        assertNothingWasRegistered();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -2})
    void shouldRefuseSeatsWithoutAPositiveQuantity(int quantity) {
        assertThrows(OrderValidationException.class, () -> useCase.execute(seatsOrder("COMPANY", quantity)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldRefuseSeatsWithoutAQuantity() {
        assertThrows(OrderValidationException.class, () -> useCase.execute(seatsOrder("COMPANY", null)));

        assertNothingWasRegistered();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentMethod.class, names = {"PIX", "BOLETO"})
    void shouldRefuseEveryPaymentMethodThatIsNotCard(PaymentMethod method) {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);

        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), method, 1)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldRefuseAnOrderWithoutPaymentMethod() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);

        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), null, 1)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldRefuseAnOrderWithoutItemType() {
        assertThrows(OrderValidationException.class, () -> useCase.execute(new CreateOrderCommand(
                buyerId, "STUDENT", null, null, null, PaymentMethod.CARD, 1)));
    }

    @Test
    void shouldRefuseInstallmentsBelowOne() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);

        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 0)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldRefuseAdminsAndUnknownProfiles() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);

        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder("ADMIN", course.getId(), PaymentMethod.CARD, 1)));
        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder(null, course.getId(), PaymentMethod.CARD, 1)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldFailWhenTheCourseDoesNotExist() {
        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(courseOrder("STUDENT", UUID.randomUUID(), PaymentMethod.CARD, 1)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldRefuseACourseOrderWithoutCourseId() {
        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder("STUDENT", null, PaymentMethod.CARD, 1)));

        assertNothingWasRegistered();
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"DRAFT", "ARCHIVED"})
    void shouldRefuseCoursesThatAreNotPublished(CourseStatus status) {
        final Course course = givenCourse(status, 100.0, 30);

        assertThrows(OrderValidationException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1)));

        assertNothingWasRegistered();
    }

    @Test
    void shouldRefuseBuyingACourseTheStudentAlreadyHasActive() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.studentEnrollments.put(key(buyerId, course), new Enrollment(TODAY.minusDays(5), TODAY.plusDays(25)));

        assertThrows(CourseAlreadyActiveException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1)));

        assertTrue(orders.saved.isEmpty());
    }

    @Test
    void shouldTreatAnEnrollmentWithoutEndDateAsActive() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.studentEnrollments.put(key(buyerId, course), new Enrollment(TODAY.minusDays(5), null));

        assertThrows(CourseAlreadyActiveException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1)));
    }

    @Test
    void shouldTreatAnEnrollmentThatEndsTodayAsStillActive() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.studentEnrollments.put(key(buyerId, course), new Enrollment(TODAY.minusDays(30), TODAY));

        assertThrows(CourseAlreadyActiveException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1)));
    }

    @Test
    void shouldRenewAnEnrollmentThatAlreadyExpired() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.studentEnrollments.put(key(buyerId, course), new Enrollment(TODAY.minusDays(60), TODAY.minusDays(1)));

        final Order order = useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1));

        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertEquals(new Enrollment(TODAY, TODAY.plusDays(30)), access.studentEnrollments.get(key(buyerId, course)));
    }

    @Test
    void shouldRefuseBuyingACourseTheCompanyAlreadyHas() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.companyCourses.put(key(buyerId, course), new Enrollment(TODAY.minusDays(5), null));

        assertThrows(CourseAlreadyActiveException.class,
                () -> useCase.execute(courseOrder("COMPANY", course.getId(), PaymentMethod.CARD, 1)));

        assertTrue(orders.saved.isEmpty());
    }

    @Test
    void shouldLetAStudentBuyACourseThatOnlyTheirCompanyOrAnotherUserHas() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.studentEnrollments.put(key(UUID.randomUUID(), course), new Enrollment(TODAY, null));
        access.companyCourses.put(key(UUID.randomUUID(), course), new Enrollment(TODAY, null));

        final Order order = useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1));

        assertEquals(OrderStatus.APPROVED, order.getStatus());
    }

    @Test
    void shouldPropagateAFailureWhileReleasingTheAccessSoTheTransactionRollsBack() {
        final Course course = givenCourse(CourseStatus.PUBLISHED, 100.0, 30);
        access.failOnSave = true;

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(courseOrder("STUDENT", course.getId(), PaymentMethod.CARD, 1)));
    }

    private static String key(UUID buyerId, Course course) {
        return buyerId + ":" + course.getId();
    }

    /** Pedidos em memória; guarda o que foi salvo para os testes conferirem. */
    private static final class FakeOrders implements OrderRepositoryPort {

        private final List<Order> saved = new ArrayList<>();

        @Override
        public Order save(Order order) {
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            saved.add(order);
            return order;
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return saved.stream().filter(order -> order.getId().equals(id)).findFirst();
        }

        @Override
        public List<Order> findByBuyerUserId(UUID buyerUserId) {
            return saved.stream().filter(order -> order.getBuyerUserId().equals(buyerUserId)).toList();
        }

        // A listagem do admin (US-28) não faz parte da criação de pedidos.
        @Override
        public PageResult<OrderSummary> findOrders(OrderFilter filter, int page, int size) {
            throw new UnsupportedOperationException("not used by CreateOrderUseCase");
        }

        @Override
        public Optional<OrderDetails> findDetailsById(UUID id) {
            throw new UnsupportedOperationException("not used by CreateOrderUseCase");
        }
    }

    /** Liberação de acesso em memória; as chaves são "comprador:curso". */
    private static final class FakeAccess implements OrderAccessPort {

        private final Map<String, Enrollment> studentEnrollments = new HashMap<>();
        private final Map<String, Enrollment> companyCourses = new HashMap<>();
        private final Map<UUID, Integer> seatsAdded = new HashMap<>();
        private boolean failOnSave;

        @Override
        public Optional<Enrollment> findStudentEnrollment(UUID userId, UUID courseId) {
            return Optional.ofNullable(studentEnrollments.get(userId + ":" + courseId));
        }

        @Override
        public void saveStudentEnrollment(UUID userId, UUID courseId, LocalDate startDate, LocalDate expirationDate) {
            failIfRequested();
            studentEnrollments.put(userId + ":" + courseId, new Enrollment(startDate, expirationDate));
        }

        @Override
        public Optional<Enrollment> findCompanyCourse(UUID companyId, UUID courseId) {
            return Optional.ofNullable(companyCourses.get(companyId + ":" + courseId));
        }

        @Override
        public void saveCompanyCourse(UUID companyId, UUID courseId, LocalDate startDate, LocalDate expirationDate) {
            failIfRequested();
            companyCourses.put(companyId + ":" + courseId, new Enrollment(startDate, expirationDate));
        }

        @Override
        public void addCompanySeats(UUID companyId, int quantity) {
            failIfRequested();
            seatsAdded.merge(companyId, quantity, Integer::sum);
        }

        private void failIfRequested() {
            if (failOnSave) {
                throw new IllegalStateException("simulated failure while releasing the access");
            }
        }
    }
}
