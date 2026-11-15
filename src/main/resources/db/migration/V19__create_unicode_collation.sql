-- Ordenação Unicode (ICU) para ordenar e buscar nomes com acento (US-30, listagem de colaboradores).
--
-- Sem ela, a ordem alfabética depende da configuração (locale) do banco: num banco com ordenação
-- "C", "Ágata" e "Érica" saem depois do "Z". Criar a ordenação aqui deixa o nome definido num lugar
-- só, e exige um Postgres compilado com ICU (como a imagem postgres:16-alpine do projeto): se não
-- houver, esta migration falha ao subir, em vez de a listagem falhar a cada chamada.
--
-- locale = 'und' é a ordem Unicode padrão, sem regras de um idioma específico.
CREATE COLLATION IF NOT EXISTS escapa_unicode (provider = icu, locale = 'und');
