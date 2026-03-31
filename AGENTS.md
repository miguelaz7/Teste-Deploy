AI Coding Agent Instructions
Objetivo

Este agente deve ajudar no desenvolvimento de um projeto fullstack com:

Backend: Java + Spring Boot
Frontend: React + CSS

Deve atuar de forma cuidadosa, explicativa e alinhada com boas práticas profissionais.

Comportamento
Antes de gerar código:
Fazer perguntas para clarificar requisitos
Não assumir detalhes importantes
Depois de gerar código:
Fornecer explicação curta e clara
Explicar o que foi feito e porquê

Tecnologias
Backend
Java
Spring Boot
Frontend
React
CSS

Estilo de Código
Código deve ser:
Escalável
Modular
Limpo e organizado
Convenções:
Variáveis em português
Comentários naturais (sem exagero nem óbvios)
Seguir boas práticas de desenvolvimento

Estrutura do Projeto (Backend)

O agente deve respeitar rigorosamente esta estrutura:

config/ → configurações (segurança, CORS, etc.)
controller/ → endpoints da API
dto/ → transferência de dados
exception/ → tratamento de erros
model/ → entidades da base de dados
repository/ → acesso a dados (JPA)
service/ → lógica de negócio

Nunca misturar responsabilidades entre camadas.

⚙️ Boas Práticas Obrigatórias
Separação de responsabilidades (MVC)
Validação de inputs
Tratamento de erros adequado
Código reutilizável
Segurança (evitar vulnerabilidades comuns)
Performance (evitar código desnecessário)
Testes
Incluir testes sempre que possível
Validar comportamento das funcionalidades principais

Restrições

O agente NÃO deve:

Inventar requisitos sem confirmação
Usar soluções demasiado complexas sem necessidade
Ignorar validação de dados
Quebrar a estrutura do projeto
Gerar código sem contexto suficiente