# MEDx Email Templates Reference

## 📋 Template Index

| #  | Template Name                     | Key Parameters                          |
|----|-----------------------------------|-----------------------------------------|
| 0  | Welcome Email                     | `nome`                                  |
| 1  | Email Verification                | `nome`, `otp`                           |
| 2  | Password Reset OTP                | `nome`, `otp`                           |
| 3  | Deposit Confirmation              | `nome`, `valor`, `moeda`, `data`        |
| 4  | Transfer Received                 | `nome`, `valor`, `moeda`, `remetente`, `data` |
| 5  | Transfer Sent                     | `nome`, `valor`, `moeda`, `destinatario`, `data` |
| 6  | Password Changed                  | `nome`                                  |
| 7  | Suspicious Login Alert            | `nome`, `local`, `dispositivo`, `data`  |
| 8  | Failed Transaction                | `nome`, `valor`, `moeda`, `destinatario`, `motivo`, `data` |
| 9  | Reversed Transaction              | `nome`, `valor`, `moeda`, `motivo`, `valor_revertido`, `data` |
| 10 | Multiple Login Attempts           | `nome`, `locais`, `dispositivos`, `horarios` |
| 11 | Support Message                   | `nome`, `mensagem`, `tipo_mensagem`, `prioridade`, `ticket_id` |
| 12 | Withdrawal Confirmation           | `nome`, `valor`, `moeda`, `conta_destino`, `codigo_rastreio`, `data`, `transaction_id` |
| 13 | Transaction Limit Reached         | `nome`, `tipo_limite`, `valor_limite`, `moeda`, `link_verificacao` |
| 14 | Service Payment Confirmation      | `nome`, `servico`, `valor`, `moeda`, `referencia`, `data`, `link_comprovante` |
| 15 | Low Balance Alert                 | `nome`, `saldo_atual`, `limite`, `moeda` |
| 16 | Group Invitation                  | `nome`, `grupo_nome`, `privilegios`, `link_aceitar`, `link_recusar` |
| 17 | Account Suspended                 | `nome`                                  |
| 18 | KYC Approved                      | `nome`                                  |
| 19 | KYC Rejected                      | `nome`, `link_login`                    |
| 20 | KYC Request                       | `nome`, `link_login`                    |

## ✨ Common Parameters
- `app_name` (optional, defaults to "MEDx")
- All dates should use format: `"dd/MM/yyyy HH:mm"`

## 🏷️ Parameter Glossary

| Parameter          | Description                          | Example Values                |
|--------------------|--------------------------------------|--------------------------------|
| `nome`             | User's full name                     | "Maria Silva"                 |
| `otp`              | One-Time Password                    | "A7B9C2"                      |
| `valor`            | Transaction amount                   | "150.50"                      |
| `moeda`            | Currency code                        | "MDX", "USD"                  |
| `data`             | Formatted date/time                  | "15/08/2023 14:30"            |
| `remetente`        | Sender's name                        | "Escola Secundária Maputo"    |
| `destinatario`     | Recipient's name                     | "Loja Electrónica Moz"        |
| `motivo`           | Reason for failure/reversal          | "Saldo insuficiente"          |
| `valor_revertido`  | Reversed amount                      | "100.00"                      |
| `locais`           | List of locations                    | "Maputo, Lisboa"              |
| `dispositivos`     | List of devices                      | "iPhone, Chrome Windows"      |
| `ticket_id`        | Support ticket reference             | "SUP-2023-00456"              |

## 💻 Usage Example

```java
Map<String, String> params = new HashMap<>();
params.put("nome", "Carlos Santos");
params.put("valor", "275.00"); 
params.put("moeda", "MDX");
params.put("data", "20/08/2023 09:15");
params.put("destinatario", "Hospital Central");

String email = templates.buildTemplate(5, params); // Transfer Sent