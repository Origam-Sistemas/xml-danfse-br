# Publicação (Repsy + GitHub Releases)

## Setup único (feito uma vez, pelo mantenedor)

1. **Conta no Repsy**: utilize o repositório Maven `origam/default`.
2. **Credenciais de publicação**: gere ou obtenha o usuário e a senha/token com acesso de
   escrita ao repositório.
3. **Secrets do repositório** (`gh secret set` ou Settings → Secrets → Actions):
   | Secret | Valor |
   |---|---|
   | `REPSY_USERNAME` | usuário do Repsy |
   | `REPSY_PASSWORD` | senha ou token do Repsy |

## Cada release

1. Atualize `<version>` no `pom.xml` e o `CHANGELOG.md`.
2. Commit + tag + push:
   ```bash
   git commit -am "vX.Y.Z: ..."
   git tag vX.Y.Z
   git push origin main vX.Y.Z
   ```
3. O workflow `release.yml` publica `br.com.origam:xml-danfse-br` no Repsy e anexa o fat-jar
   da CLI ao GitHub Release.

## Publicação manual (sem Actions)

```bash
# ~/.m2/settings.xml precisa do server id "repsy" com as credenciais
mvn clean deploy
mvn -Pcli -DskipTests package   # target/xml-danfse-br-cli.jar
```

> **Licenças**: o jar publicado no Repsy é MIT puro (OpenHTMLtoPDF/LGPL fica como
> dependência declarada). O fat-jar da CLI agrega bytecode LGPL, por isso é distribuído
> **somente** via GitHub Releases, acompanhado do `THIRD-PARTY.md`.
