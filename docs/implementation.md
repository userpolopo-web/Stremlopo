# Arquitetura e plano — primeira versão

Requisitos: Android, tema preto/cinza, exclusivamente fontes autorizadas, player integrado e gestão centralizada.

1. Catálogo persistente (`backend/catalog.py`): testar bloqueio de publicação, retirada após edição, denúncias, remoção e restrição comercial; implementar SQLite e auditoria.
2. HTTP (`backend/server.py`): separar leitura pública e administração; token somente no servidor; painel de cadastro estruturado. Testar autenticação e persistência por requisições reais.
3. Android (`android/app`): Compose, catálogo HTTPS, busca, detalhes, favoritos e histórico locais; Media3 MP4/HLS com retomada e ciclo de vida.
4. Entrega (`.github/workflows/ci.yml`): Python unittest e Gradle assembleDebug/lintDebug; disponibilizar APK debug via Actions.

Não publicar conteúdo de demonstração sem evidência real. Não adicionar monetização habilitada por padrão. Não apresentar este código inicial como serviço hospedado ou APK validado em dispositivo.

Próximas etapas estão listadas no README. A revisão humana da licença é um controle administrativo, não uma determinação jurídica automática.
