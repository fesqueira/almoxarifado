# Almoxarifado

Sistema de controle de estoque desenvolvido em Java para praticar e aplicar conceitos de programação orientada a objetos, banco de dados, persistência de dados e desenvolvimento de interfaces gráficas.

O projeto começou como uma aplicação de console e foi evoluindo até possuir uma interface gráfica em Java Swing, permitindo cadastrar produtos e controlar o estoque de forma mais simples.

## Sobre o projeto

O sistema permite realizar as principais operações de um pequeno almoxarifado:

* Cadastro de produtos
* Listagem de produtos
* Aumento de estoque
* Diminuição de estoque
* Exclusão de produtos
* Controle de estoque mínimo
* Identificação visual de produtos abaixo do estoque mínimo
* Persistência dos dados em banco de dados
* Interface gráfica utilizando Java Swing

Cada produto possui informações como nome, descrição, preço de compra, preço de venda, quantidade disponível e estoque mínimo.

## Tecnologias utilizadas

* Java
* Java Swing
* Maven
* JDBC
* SQLite
* PostgreSQL
* JUnit
* Git e GitHub

## Estrutura

O projeto foi organizado separando as principais responsabilidades da aplicação:

```text
src/
├── main/
│   └── java/
│       └── com/
│           └── almoxarifado/
│               ├── database/
│               │   ├── postgres/
│               │   └── sqlite/
│               ├── model/
│               ├── service/
│               └── ui/
│
└── test/
    └── java/
        └── com/
            └── almoxarifado/
```

A camada `model` contém as entidades do sistema, `service` concentra as regras de negócio, `database` cuida da comunicação com o banco e `ui` contém as interfaces de interação com o usuário.

## Banco de dados

O projeto possui suporte para SQLite e PostgreSQL.

Para uma utilização local e mais simples, o SQLite pode ser utilizado sem a necessidade de instalar um servidor de banco de dados.

Na versão PostgreSQL, as informações de conexão são obtidas através de variáveis de ambiente:

```text
DB_URL
DB_USER
DB_PASSWORD
```

A senha não fica armazenada diretamente no código.

## Executando o projeto

Clone o repositório:

```bash
git clone https://github.com/fesqueira/almoxarifado.git
```

Entre na pasta:

```bash
cd almoxarifado
```

Compile o projeto utilizando Maven:

```bash
./mvnw clean package
```

No Linux, caso seja necessário dar permissão ao Maven Wrapper:

```bash
chmod +x mvnw
```

Depois execute a aplicação através da classe principal configurada no projeto.

## Testes

O projeto também possui testes utilizando JUnit para algumas das funcionalidades e regras de negócio.

Para executar os testes:

```bash
./mvnw test
```

## Objetivo

Esse projeto foi desenvolvido principalmente como forma de aprendizado e prática de Java, buscando aplicar conceitos que vão além de exercícios simples, como:

* Programação Orientada a Objetos
* Separação de responsabilidades
* Padrão de acesso a dados
* JDBC
* Persistência de informações
* Tratamento de exceções
* Testes automatizados
* Desenvolvimento de interfaces gráficas
* Integração com banco de dados
* Organização de um projeto Maven

O projeto continua podendo receber melhorias e novas funcionalidades conforme meu aprendizado em Java evolui.

## Autor

**Felipe Siqueira**

Projeto desenvolvido para estudos e prática de desenvolvimento de software com Java.
