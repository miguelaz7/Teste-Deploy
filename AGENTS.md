# AGENTS.md

## Objetivo
Este agente apoia o desenvolvimento de um sistema fullstack com:
- Backend: Java + Spring Boot
- Frontend: React + CSS

Deve agir com clareza, segurança e foco em qualidade de producao.

## Prioridade de Regras
Quando houver conflito entre orientacoes, aplicar nesta ordem:
1. Seguranca
2. Validacao de dados
3. Arquitetura e separacao de responsabilidades
4. Correcao funcional
5. Performance
6. Estilo e convencoes

## Modo de Trabalho do Agente

### Antes de gerar codigo
- Confirmar requisitos ambiguuos com perguntas objetivas.
- Nao inventar regras de negocio.
- Sinalizar riscos de arquitetura antes da implementacao.

### Depois de gerar codigo
- Explicar de forma curta: o que foi feito e por que.
- Informar impactos e trade-offs.
- Sugerir melhorias apenas quando fizer sentido.

## Stack Tecnologico

### Backend
- Java
- Spring Boot
- JPA/Hibernate

### Frontend
- React
- CSS (modules ou global organizado)
- Fetch API ou Axios (preferencialmente centralizado em services)

## Regras Obrigatorias por Camada

### Backend
Estrutura esperada:
- config/: seguranca, CORS, configuracoes globais
- controller/: endpoints HTTP
- dto/: objetos de entrada e saida
- exception/: tratamento de erros
- model/: entidades
- repository/: acesso a dados (JPA)
- service/: regras de negocio

Regras:
- Controllers nao contem regra de negocio.
- Services nao fazem acesso direto a HTTP.
- Repositories nao devem conter regra de negocio.
- Input de endpoint deve ser validado (@Valid, @NotNull, etc.).
- Erros devem ser tratados globalmente (@ControllerAdvice).
- Nao expor entidades diretamente na API quando DTO for necessario.

### Frontend
Estrutura esperada:
- src/assets/: imagens, icones, fontes
- src/components/: componentes reutilizaveis
- src/pages/: composicao de paginas
- src/services/: comunicacao com API
- src/hooks/: logica reutilizavel
- src/context/: estado global
- src/utils/: utilitarios
- src/styles/: estilos globais

Regras:
- Evitar chamadas API diretamente em JSX.
- Centralizar chamadas HTTP em services/.
- Paginas organizam fluxo; componentes focam UI.
- Tratar sempre estados de loading, erro e sucesso.
- Validar dados antes de enviar para backend.
- Evitar componentes gigantes; dividir por responsabilidade.

## Tabela de Decisao Rapida

| Tema | Obrigatorio | Proibido | Excecao |
| --- | --- | --- | --- |
| Backend controller | Receber request, validar, delegar ao service | Regra de negocio no controller | Nenhuma |
| Backend service | Implementar regra de negocio | Acesso HTTP direto | Nenhuma |
| Frontend API | Chamar API em services | fetch/axios espalhado em componentes grandes | Componentes pequenos e isolados, quando justificavel |
| Validacao | Validar input antes de persistir/enviar | Aceitar payload sem validacao | Nenhuma |
| Erros | Resposta clara e padronizada | Silenciar excecoes | Nenhuma |

## Convencoes de Codigo
- Variaveis e funcoes de negocio em portugues quando aplicavel.
- Nomes de classes Java em PascalCase.
- Funcoes e variaveis JavaScript/React em camelCase.
- Funcoes pequenas, reutilizaveis e sem duplicacao.
- Comentarios apenas quando agregam contexto real.

Exemplo de convencao:
```js
const utilizadorLogado = true;
```

## CSS e UI
- Preferir CSS Modules para isolamento.
- Evitar inline styles, salvo casos pontuais e justificados.
- Garantir responsividade mobile-first.
- Manter consistencia visual entre componentes.

## Seguranca
- Nao expor dados sensiveis no frontend.
- Evitar uso inseguro de localStorage para dados criticos.
- Preferir cookies httpOnly quando aplicavel.
- Sanitizar e validar entradas de utilizador.
- No backend, prevenir SQL Injection e validar dados no servidor.

## Fluxo de Dados Padrao
1. Utilizador interage com a UI.
2. Pagina/componente chama service.
3. Service comunica com backend.
4. Estado da aplicacao e atualizado.
5. UI re-renderiza com feedback adequado.

## Testes Minimos Obrigatorios

### Backend
- Testes unitarios para services.
- Testes de integracao para controllers.
- Cobrir fluxos principais e validacoes criticas.

### Frontend
- Usar Jest + React Testing Library.
- Testar renderizacao, interacoes e fluxo principal.
- Validar comportamento de loading, erro e sucesso.

## Definition of Done (DoD)
Uma tarefa so esta concluida quando:
- Regras de arquitetura foram respeitadas.
- Validacoes foram implementadas.
- Tratamento de erro foi aplicado.
- Fluxo principal foi testado.
- Codigo esta claro, modular e sem complexidade desnecessaria.

## Restricoes (Nao Fazer)
- Nao inventar requisitos sem confirmacao.
- Nao misturar responsabilidades entre camadas.
- Nao ignorar validacao de dados.
- Nao ignorar tratamento de erros.
- Nao criar codigo desnecessariamente complexo.
- Nao ignorar seguranca e performance.

## Boas Praticas Profissionais
- Clareza acima de complexidade.
- Codigo limpo e evolutivo.
- Reutilizacao de logica sempre que possivel.
- Pensar como sistema real em producao.
