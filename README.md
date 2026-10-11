# Stremlopo

Aplicativo Android de cinema com tema preto e cinza. A versão 0.2 abre diretamente com quatro filmes de licença aberta, capas e sinopses incluídas, destaque, fileiras por categoria e uma grade com todo o catálogo. Não exige servidor ou cadastro de capas para começar.

Títulos incluídos: **Sintel**, **Big Buck Bunny**, **Tears of Steel** e **Elephants Dream**. As fontes e licenças estão documentadas em [docs/catalog-sources.md](docs/catalog-sources.md). Os vídeos são transmitidos pela internet; o aplicativo inclui os metadados e imagens, não os arquivos de filmes.

## O que está implementado

- Android Kotlin/Compose: catálogo, busca por título/gênero, detalhes e capas, reprodução MP4/HLS com Media3, seleção entre fontes, minha lista, histórico e retomada local.
- Player Media3: controles, avanço/retrocesso e seleção de faixas disponibilizadas pela mídia. Áudio e legendas dependem da fonte.
- API Python/SQLite: catálogo público separado de rascunhos, administração com token, cadastro/edição/remoção, denúncias e trilha de auditoria.
- Painel web preto e cinza em `/admin`: cadastro estruturado em JSON, edição, publicação e remoção; consulta de denúncias.
- Publicação exige titular, licença, evidência, créditos, responsável pela revisão e autorização de reprodução integrada. Monetização exige permissão comercial. Qualquer edição revoga a publicação.
- GitHub Actions testa o backend e compila um APK de desenvolvimento.

## Executar servidor local

Python 3.12 ou superior, sem dependências externas:

```bash
cd backend
export ADMIN_TOKEN="$(python -c 'import secrets; print(secrets.token_urlsafe(32))')"
python server.py
```

Abra `http://127.0.0.1:8080/admin`. Cole no painel o token gerado no seu ambiente. Não coloque tokens no código ou no GitHub. O token existe apenas em memória no painel.

Para celulares acessarem o servidor, hospede a API atrás de um proxy HTTPS. Configure `HOST=0.0.0.0` apenas no ambiente protegido pelo proxy. O aplicativo rejeita HTTP. Para adicionar seu catálogo ao incluído, informe o endereço HTTPS na engrenagem **Configurações**. Essa opção é facultativa e fica fora da tela inicial. Não há servidor de produção hospedado por este repositório.

SQLite usa `DATABASE_PATH` (padrão `catalog.db`). Mantenha este arquivo em um volume persistente com backup. `PORT` padrão: 8080.

## Compilar Android

Android SDK 35, JDK 17, Gradle 8.9:

```bash
cd android
gradle testDebugUnitTest assembleDebug lintDebug
```

O workflow **Testes e APK Android** disponibiliza `Stremlopo-APK-debug` em **Actions** quando a compilação passa. APK debug serve para avaliação; uma distribuição exige assinatura release e configuração de infraestrutura. Sem Gradle Wrapper nesta versão; use a versão indicada ou o workflow.

## Testes

```bash
python -m unittest discover -s backend/tests -v
```

## Revisão de direitos

A verificação é feita por um administrador, com evidências conservadas no cadastro. Uma caixa marcada não comprova legalidade. Confirme os direitos aplicáveis ao Brasil, direitos sobre áudio/legendas/capas, permissões de integração, atribuição, restrições territoriais, prazo e uso comercial antes de aprovar. Conteúdo de domínio público em outro país não é automaticamente de domínio público no Brasil. APIs de metadados não concedem licença para exibir o filme.

O servidor não faz download, scraping, proxy de vídeo ou remoção de DRM. A reprodução é feita diretamente pelo Android na fonte autorizada. Disponibilidade pública não substitui autorização. A equipe deve revisar as denúncias e remover o conteúdo quando necessário.

## Limites desta versão

Esta é uma base funcional inicial, não uma plataforma pronta para produção. Ainda faltam: importação de metadados por APIs, verificação automática da disponibilidade dos links, modelagem completa de temporadas/episódios, recomendações personalizadas, sincronização entre dispositivos, formulários administrativos amigáveis, revisão de licenças com prazo/território estruturados e endurecimento de produção (contas administrativas, rate limiting e monitoramento). A duração é informada em minutos.

Consulte o resultado real do workflow: ele compila o Android, executa os testes de busca/catálogo, verifica lint e testa a primeira abertura e a renderização de vídeo em um emulador. Capturas reais da interface ficam no artefato `Stremlopo-interface`. O emulador não substitui um teste no seu aparelho. Nenhum APK está incluído no código-fonte. A licença dos conteúdos deve permitir também o acesso técnico pelo dispositivo; certos provedores não autorizam integração.
