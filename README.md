# My Heart

Aplicativo Android que monitora **batimentos cardíacos (BPM)** e **oxigenação do sangue (SpO₂)** a partir de um sensor Bluetooth de baixa energia (BLE), classifica o resultado em três níveis e avisa o usuário (e, se ele quiser, um familiar) quando a leitura sai do normal.

Desenvolvido em **Kotlin** com **Jetpack Compose**, **Navigation Compose**, **Room** e arquitetura **MVVM**.

> ⚠️ **Aviso:** projeto acadêmico. O My Heart **não é um dispositivo médico**, não faz diagnóstico e não substitui acompanhamento profissional. Os limiares de classificação são ilustrativos.

---

## Informações da entrega

| Item | Informação |
| --- | --- |
| Aluno | Jean Morais da Silva |
| Turma | Capacitação iRede — Android — [CBIREDE] [C2] |
| Matrícula | Não se aplica (identificação pela turma) |
| Projeto | Projeto final do módulo de desenvolvimento Android |
| Data de entrega | 02/11/2026 |

---

## Justificativa do tema

Como estudante de Engenharia de Computação, venho construindo um sistema embarcado capaz de medir batimentos cardíacos e oxigenação sanguínea. Esse equipamento, porém, só se torna útil para uma pessoa comum quando seus números chegam a uma tela simples, que dispense qualquer conhecimento técnico para ser entendida. O My Heart nasceu dessa necessidade: ser a interface do meu hardware, trazendo as medições para o celular, mostrando quando algo foge do esperado e guardando o histórico para consulta posterior.

A escolha também tem um motivo didático. Receber dados de um dispositivo real por Bluetooth é um desafio que vai além de listas e formulários: exige lidar com permissões, conexões assíncronas, falhas de comunicação e dados que chegam aos poucos. Resolver isso, mantendo ao mesmo tempo o código organizado em camadas (MVVM), com navegação entre telas e persistência local com Room, permitiu aplicar o conteúdo do módulo em um problema concreto e ligar duas áreas que estudo: sistemas embarcados e desenvolvimento Android.

Desde o início, a ideia foi que o app não ficasse preso ao meu protótipo: ele foi pensado para aceitar outros medidores que sigam os padrões do Bluetooth, e a ampliação para mais aparelhos é um caminho de evolução natural.

O planejamento inicial (wireframes, estados e navegação) foi entregue em 21/07/2026. O aplicativo final seguiu esse esboço e acrescentou histórico, notificações e aviso ao familiar.

---

## Como o aplicativo funciona

O app tem **cinco telas**, com uma barra de navegação inferior (Início, Instruções, Perfil). A Leitura e o Histórico fazem parte do fluxo da tela Início.

### 1. Início
- Mostra o estado da conexão com o medidor: desconectado, conectando, erro (com o motivo) ou conectado (com o nome do dispositivo e o botão **Desconectar**).
- Exibe a última leitura de BPM e SpO₂, com horário ("Hoje", "Ontem" ou a data). A última leitura gravada reaparece ao reabrir o app.
- Botão **Realizar leitura agora**, habilitado só com um medidor conectado.
- Gráfico de barras com uma barra por leitura do dia (alternando BPM e oxigenação), colorida pelo nível, e estatísticas de média, máxima e mínima.
- Acesso ao **Histórico de medições**.

### 2. Leitura atual
- **Em andamento:** indicador de progresso e botão para cancelar.
- **Concluída:** valores lidos e análise por nível (Normal, Atenção ou Crítico), com cores.
- **Erro:** mensagem específica (sem sinal, conexão perdida etc.), com opção de tentar de novo sem reconectar.
- Se a leitura não puder ser gravada, o app avisa e oferece **Salvar de novo**.
- Em leituras fora do normal, aparece a opção de **avisar o familiar** (veja abaixo).

### 3. Histórico
- Lista as leituras salvas, agrupadas por dia, com filtros de **7 dias**, **30 dias** ou **todo o período**.

### 4. Instruções
- Passo a passo de uso e de conexão, ajuda para problemas comuns e aviso legal.

### 5. Perfil
- **Seus dados:** nome, e-mail e o switch "Avisar em caso de anormalidade" (notificação no próprio celular).
- **Familiar:** nome, canal de contato (e-mail ou telefone), contato e **autorização expressa** para ser avisado.
- Os dados são validados antes de serem salvos e permanecem no aparelho.

### Avisos de leitura fora do normal

| Aviso | Como funciona |
| --- | --- |
| **Notificação local** | Se o switch estiver ligado, uma leitura Atenção ou Crítica gera uma notificação no celular. Na tela de bloqueio aparece um texto genérico, **sem os valores de saúde**. |
| **Aviso ao familiar** | Se houver contato salvo **e** autorização, o app oferece abrir o app de e-mail ou de mensagens com o texto já preenchido. **O app não envia nada sozinho**: o usuário confere e confirma o envio. Por isso não exige permissões sensíveis (como SMS) nem servidor. Se o aparelho não tiver app para isso, é possível copiar a mensagem. |

---

## Níveis de criticidade

A classificação usa os valores de referência abaixo, **iguais para todos os usuários**. O nível geral da leitura é o **pior** entre o nível dos batimentos e o da oxigenação.

| Medida | Normal | Atenção | Crítico |
| --- | --- | --- | --- |
| Batimentos (BPM) | 60 a 100 | 40 a 59 ou 101 a 130 | abaixo de 40 ou acima de 130 |
| Oxigenação (SpO₂) | 95% ou mais | 90% a 94% | abaixo de 90% |

> **Limitação importante:** o app **ainda não considera idade, peso nem atividade física**. A frequência cardíaca esperada muda muito conforme o perfil da pessoa e o momento (repouso, exercício, atletas, crianças, idosos), então a criticidade é baseada em faixas **gerais**. Os limiares são ilustrativos e não foram validados clinicamente.

---

## Bluetooth e dispositivos compatíveis

A conexão é feita por **BLE, sem pareamento e sem tela de seleção**. Ao tocar no aviso de conexão, o app:

1. pede as permissões necessárias e confere se o Bluetooth está ligado;
2. tenta reconectar ao **último medidor usado**;
3. se não der, procura por até 8 segundos um dispositivo compatível e escolhe o de sinal mais forte;
4. valida os serviços depois de conectar.

A leitura junta **5 amostras válidas** de cada medida e grava a **mediana**, descartando valores fora de faixa, dedo removido e pacotes sem sinal. Se não houver leitura estável em 30 s, a leitura falha com "sem sinal".

### Compatibilidade

O aplicativo funciona **somente com dispositivos que seguem os perfis padrão da Bluetooth SIG**:

| Perfil | Serviço | Característica usada |
| --- | --- | --- |
| Heart Rate | `0x180D` | Heart Rate Measurement (`0x2A37`) |
| Pulse Oximeter | `0x1822` | PLX Continuous Measurement (`0x2A5F`) |

Como o modelo de leitura exige BPM e SpO₂, o dispositivo precisa oferecer a medição contínua do **Pulse Oximeter**. Dispositivos que medem só frequência cardíaca são recusados como incompatíveis.

**Dispositivos de fabricantes como Samsung, Apple e Xiaomi não são suportados no momento**, pois usam especificações e protocolos próprios. O suporte a eles está previsto como evolução futura: a leitura é acessada por uma interface (`SensorDataSource`), então novos parsers ou adaptadores podem ser adicionados sem alterar as telas.

O dispositivo de referência, usado em todos os testes, é o embarcado do autor: **ESP32-C3 Super Mini + sensor MAX30102** (firmware 2.1.0).

---

## Arquitetura

Organização em três camadas, com **MVVM** e fluxo de dados unidirecional (o estado desce do ViewModel e os eventos sobem da tela).

```
app/src/main/java/com/example/myheart/
├── ui/            Telas Compose, ViewModels, navegação, componentes e tema
├── domain/        Modelos, contratos (repositórios, sensor, notificador) e casos de uso
└── data/
    ├── local/         Room: entidades, DAOs, migrações e banco
    ├── repository/    Implementações dos repositórios
    ├── sensor/        Contrato do sensor, coleta de amostras, sensor simulado e BLE
    └── notification/  Notificação local
```

Decisões principais:

- **Sensor por interface:** `SensorDataSource` tem uma implementação BLE (em uso) e uma simulada (`FakeSensorDataSource`, para demonstração e testes).
- **Repositórios com interface no domínio:** o Room fica restrito à camada de dados, com conversão entre modelo de banco e modelo de domínio.
- **Estado reativo:** o gráfico e o Histórico leem consultas observáveis (`Flow`) e se atualizam sozinhos quando uma leitura é gravada.
- **Navegação tipada:** rotas com Navigation Compose e serialização.
- **Injeção de dependência manual** (sem Hilt), para reduzir a configuração.
- **Falhas visíveis:** erros do banco, da conexão e da leitura aparecem ao usuário e nunca derrubam o app nem confirmam um dado que não foi gravado.

### Persistência (Room)

Banco `myheart.db` (versão 4) com as tabelas `profile`, `relative_contact` e `reading`, e três migrações escritas à mão (1→2, 2→3 e 3→4). O último medidor usado fica em preferências privadas, fora do banco.

---

## Tecnologias

| Componente | Versão |
| --- | --- |
| Kotlin | 2.2.10 |
| Android Gradle Plugin | 9.3.3 |
| Gradle | 9.5.0 |
| Jetpack Compose (BOM) | 2026.02.01 (Material 3) |
| Navigation Compose | 2.8.5 |
| Room (com KSP 2.3.10) | 2.8.5 |
| Lifecycle / Activity Compose | 2.10.0 / 1.13.0 |
| `minSdk` / `targetSdk` | 28 / 36 |

As versões ficam concentradas em `gradle/libs.versions.toml`.

---

## Como executar

**Requisitos:** Android Studio compatível com o AGP 9.3 e JDK 11 ou superior.

1. Clone o repositório e abra a pasta do projeto no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Execute em um **aparelho físico** (Android 9 ou superior).

> O emulador **não serve para testar o Bluetooth**. Para ver o app sem hardware, é possível usar o sensor simulado (`FakeSensorDataSource`) trocando a montagem na `MainActivity`; as leituras simuladas são rotuladas como "ilustrativas".

**Permissões pedidas ao tocar em conectar:**

- Android 12 ou superior: *Dispositivos próximos* (`BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT`).
- Android 9 a 11: *Localização* (necessária para buscar dispositivos BLE; mantenha também a localização do celular ligada).
- Android 13 ou superior: *Notificações*, pedida ao ligar o switch de aviso no Perfil.

**Testes unitários:**

```bash
./gradlew testDebugUnitTest
```

São **128 testes** (classificação, casos de uso, ViewModels, decodificação BLE, coleta de amostras, escolha de dispositivo e aviso ao familiar), executados sem Android, com repositórios e sensor falsos.

---

## Privacidade e segurança

Por lidar com dados de saúde, o app segue o princípio de guardar o mínimo possível:

- **Sem login, senha ou servidor:** nada sai do aparelho, exceto o que o usuário confirma enviar ao familiar.
- **Backup automático e transferência entre aparelhos desligados.**
- **Contato do familiar com minimização de dados:** só é gravado o dado do canal escolhido e o consentimento.
- **Notificação privada:** a tela de bloqueio não mostra BPM nem SpO₂.
- **Sem permissões sensíveis:** o aviso ao familiar usa o app de e-mail ou de mensagens do usuário.

---

## Limitações conhecidas

- **Não é um dispositivo médico.** Limiares ilustrativos, sem validação clínica.
- **Criticidade geral:** não considera idade, peso nem atividade física.
- **Compatibilidade Bluetooth restrita** aos perfis Heart Rate (`0x180D`) e Pulse Oximeter (`0x1822`); relógios e pulseiras de Samsung, Apple, Xiaomi e outros com protocolo próprio não são suportados.
- Dispositivos que medem só BPM, ou só a medição pontual do oxímetro, são recusados.
- No Android 11 ou anterior, a busca exige a localização do celular ligada.
- O aviso ao familiar é **manual** (o usuário confirma o envio); não há envio automático.
- Tema **somente claro**.
- **Banco sem criptografia** (dados protegidos pelo isolamento do Android e pelo backup desligado).
- Sem teste automatizado da camada Bluetooth (depende de hardware); ela foi validada por roteiro manual no aparelho.
- A conexão BLE pode cair sem aviso com o app parado; a queda é percebida na próxima leitura.

---

## Evoluções futuras

- Considerar **idade, peso e nível de atividade** na classificação.
- Suporte a **relógios e pulseiras** (Samsung, Apple, Xiaomi) por novos parsers atrás de `SensorDataSource`.
- Aceitar dispositivos que medem **só BPM** (SpO₂ opcional no modelo).
- Tema escuro.
- Validação dos limiares com fonte clínica.

---

## Capturas de tela

| Início sem medidor | Início com medidor conectado |
| --- | --- |
| ![Início sem medidor conectado](docs/screenshots/inicio-desconectado.png) | ![Início com medidor conectado](docs/screenshots/inicio-conectado.png) |

| Gráfico de batimentos | Gráfico de oxigenação |
| --- | --- |
| ![Gráfico de batimentos (BPM)](docs/screenshots/grafico-bpm.png) | ![Gráfico de oxigenação (SpO₂)](docs/screenshots/grafico-oxigenacao.png) |

| Leitura concluída |
| --- |
| ![Leitura concluída com análise](docs/screenshots/leitura-concluida.png) |

--- | --- | --- |
| ![Início](docs/screenshots/inicio.png) | ![Leitura](docs/screenshots/leitura.png) | ![Histórico](docs/screenshots/historico.png) |

| Instruções | Perfil | Aviso ao familiar |
| --- | --- | --- |
| ![Instruções](docs/screenshots/instrucoes.png) | ![Perfil](docs/screenshots/perfil.png) | ![Aviso ao familiar](docs/screenshots/aviso-familiar.png) |

---

## Autor

**Jean Morais da Silva**
Capacitação iRede — Android — [CBIREDE] [C2]
