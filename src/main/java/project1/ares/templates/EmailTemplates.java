package project1.ares.templates;

import java.util.Map;

public class EmailTemplates {
    public static final String WELCOME_TITLE = "Bem-vindo(a) ao MEDx!";
    public static final String APP_NAME = "MEDx";

    public final String DEFAULT_CONTENT = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: Arial, sans-serif;
                    background-color: #f4f4f4;
                    margin: 0;
                    padding: 0;
                }
                .email-container {
                    max-width: 600px;
                    margin: 20px auto;
                    background-color: #ffffff;
                    border-radius: 10px;
                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    overflow: hidden;
                }
                .email-header {
                    background-color: #00c951;
                    color: #ffffff;
                    text-align: center;
                    padding: 20px 10px;
                }
                .email-header h1 {
                    margin: 0;
                    font-size: 24px;
                }
                .email-content {
                    padding: 20px;
                }
                .email-content h2 {
                    color: #333333;
                    margin-bottom: 10px;
                }
                .email-content p {
                    color: #555555;
                    line-height: 1.6;
                }
                .email-otp {
                    font-size: 20px;
                    font-weight: bold;
                    color: #00c951;
                    text-align: center;
                    margin: 20px 0;
                }
                .email-button {
                    display: block;
                    background-color: #00c951;
                    color: #ffffff;
                    padding: 10px 20px;
                    border-radius: 5px;
                    text-decoration: none;
                    font-size: 16px;
                    margin: 20px auto;
                    text-align: center;
                }
                .email-footer {
                    background-color: #f4f4f4;
                    text-align: center;
                    color: #999999;
                    font-size: 12px;
                    padding: 10px 20px;
                }
            </style>
        </head>
        <body>
            <div class="email-container">
                <div class="email-header">
                    <h1>{{app_name}}</h1>
                    <p>Transformando pagamentos e carteiras digitais</p>
                </div>
                <div class="email-content">
                    {{email_content}}
                </div>
                <div class="email-footer">
                    <p>© 2025 {{app_name}}. Todos os direitos reservados.</p>
                    <p>Este e-mail é automático. Por favor, não responda.</p>
                </div>
            </div>
        </body>
        </html>
        """;

    // 0 - Boas-vindas
    public final String WELCOME_TEMPLATE = """
        <h2>Bem-vindo(a) ao {{app_name}}!</h2>
        <p>Olá {{nome}},</p>
        <p>Estamos felizes em tê-lo(a) na nossa carteira digital. Com o MEDx, você poderá enviar, receber e gerenciar seus fundos de forma segura e prática.</p>
        <a href="https://medx.com/dashboard" class="email-button">Acessar Carteira</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 1 - Verificação de e-mail
    public final String EMAIL_VERIFICATION_TEMPLATE = """
        <h2>Verificação de E-mail</h2>
        <p>Olá {{nome}},</p>
        <p>Para ativar sua conta no {{app_name}}, utilize o código de verificação abaixo:</p>
        <div class="email-otp">{{otp}}</div>
        <p>Este código expira em 10 minutos.</p>
        <p>Se você não criou uma conta, ignore este e-mail.</p>
        <p>Equipe {{app_name}}</p>
        """;

    // 2 - Recuperação de senha
    public final String PASSWORD_RESET_OPT_TEMPLATE = """
        <h2>Redefinição de Senha</h2>
        <p>Olá {{nome}},</p>
        <p>Você solicitou a redefinição da sua senha na carteira {{app_name}}. Use o código abaixo:</p>
        <div class="email-otp">{{otp}}</div>
        <p>Este código é válido por 15 minutos.</p>
        <p>Se não foi você, ignore este e-mail.</p>
        <p>Equipe {{app_name}}</p>
        """;

    // 3 - Confirmação de depósito
    public final String DEPOSIT_CONFIRMATION_TEMPLATE = """
        <h2>Depósito Recebido</h2>
        <p>Olá {{nome}},</p>
        <p>Recebemos seu depósito de <strong>{{valor}}</strong> {{moeda}} na sua carteira MEDx.</p>
        <p>Data: {{data}}</p>
        <p>Agora você pode usar seus fundos para transferências ou pagamentos.</p>
        <a href="https://medx.com/transactions" class="email-button">Ver Transações</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 4 - Transferência recebida
    public final String TRANSFER_RECEIVED_TEMPLATE = """
        <h2>Transferência Recebida</h2>
        <p>Olá {{nome}},</p>
        <p>Você recebeu <strong>{{valor}}</strong> {{moeda}} de {{remetente}}.</p>
        <p>Data: {{data}}</p>
        <a href="https://medx.com/transactions" class="email-button">Ver Carteira</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 5 - Transferência enviada
    public final String TRANSFER_SENT_TEMPLATE = """
        <h2>Transferência Enviada</h2>
        <p>Olá {{nome}},</p>
        <p>Você enviou <strong>{{valor}}</strong> {{moeda}} para {{destinatario}}.</p>
        <p>Data: {{data}}</p>
        <a href="https://medx.com/transactions" class="email-button">Ver Carteira</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 6 - Senha alterada
    public final String PASSWORD_CHANGED_TEMPLATE = """
        <h2>Senha Alterada com Sucesso</h2>
        <p>Olá {{nome}},</p>
        <p>Informamos que sua senha na carteira {{app_name}} foi alterada com sucesso.</p>
        <p>Se não foi você, redefina sua senha imediatamente.</p>
        <p>Equipe {{app_name}}</p>
        """;

    // 7 - Acesso suspeito
    public final String SUSPICIOUS_LOGIN_TEMPLATE = """
        <h2>Alerta de Segurança</h2>
        <p>Olá {{nome}},</p>
        <p>Detectamos um acesso à sua conta de um novo dispositivo/local:</p>
        <p><strong>Local:</strong> {{local}}<br><strong>Dispositivo:</strong> {{dispositivo}}<br><strong>Data:</strong> {{data}}</p>
        <p>Se não foi você, altere sua senha imediatamente.</p>
        <p>Equipe {{app_name}}</p>
        """;


    // 🔹 Novos Templates para casos de uso avançados
    public final String KYC_REQUEST_TEMPLATE = """
        <h2>Solicitação de KYC</h2>
        <p>Olá {{nome}},</p>
        <p>Para continuar usando a sua conta e desbloquear todos os recursos do MEDx, por favor complete a verificação de identidade (KYC).</p>
        <a href="{{link_login}}" class="email-button">Enviar Documentos</a>
        <p>Equipe {{app_name}}</p>
        """;

    public final String KYC_APPROVED_TEMPLATE = """
        <h2>KYC Aprovado</h2>
        <p>Olá {{nome}},</p>
        <p>Parabéns! Sua verificação de identidade foi aprovada. Agora você pode usar todos os recursos da sua carteira MEDx.</p>
        <a href="https://medx.com/dashboard" class="email-button">Acessar Carteira</a>
        <p>Equipe {{app_name}}</p>
        """;

    public final String KYC_REJECTED_TEMPLATE = """
        <h2>KYC Rejeitado</h2>
        <p>Olá {{nome}},</p>
        <p>Infelizmente, sua verificação de identidade foi rejeitada. Por favor, envie documentos válidos para que possamos processar sua conta.</p>
        <a href="{{link_login}}" class="email-button">Reenviar Documentos</a>
        <p>Equipe {{app_name}}</p>
        """;

    public final String ACCOUNT_SUSPENDED_TEMPLATE = """
        <h2>Conta Suspensa</h2>
        <p>Olá {{nome}},</p>
        <p>Sua conta foi temporariamente suspensa devido a atividades suspeitas.</p>
        <p>Para mais informações, entre em contato com o suporte:</p>
        <a href="https://medx.com/support" class="email-button">Suporte MEDx</a>
        <p>Equipe {{app_name}}</p>
        """;


    // 8 - Transação Falhada
    public final String FAILED_TRANSACTION_TEMPLATE = """
        <h2>Transação Não Processada</h2>
        <p>Olá {{nome}},</p>
        <p>A transação de <strong>{{valor}}</strong> {{moeda}} para {{destinatario}} falhou.</p>
        <p><strong>Motivo:</strong> {{motivo}}</p>
        <p>Data: {{data}}</p>
        <a href="https://medx.com/support" class="email-button">Contatar Suporte</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 9 - Transação Revertida
    public final String REVERSED_TRANSACTION_TEMPLATE = """
        <h2>Transação Revertida</h2>
        <p>Olá {{nome}},</p>
        <p>A transação de <strong>{{valor}}</strong> {{moeda}} foi revertida.</p>
        <p><strong>Motivo:</strong> {{motivo}}</p>
        <p>Valor creditado: {{valor_revertido}} {{moeda}}</p>
        <p>Data: {{data}}</p>
        <a href="https://medx.com/transactions" class="email-button">Ver Carteira</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 10 - Bloqueio por Múltiplos Logins
    public final String MULTIPLE_LOGIN_ATTEMPTS_TEMPLATE = """
        <h2>Atividade Suspeita Detectada</h2>
        <p>Olá {{nome}},</p>
        <p>Identificamos múltiplas tentativas de login na sua conta:</p>
        <ul>
            <li><strong>Localizações:</strong> {{locais}}</li>
            <li><strong>Dispositivos:</strong> {{dispositivos}}</li>
            <li><strong>Horários:</strong> {{horarios}}</li>
        </ul>
        <p>Sua conta foi temporariamente bloqueada por segurança.</p>
        <a href="https://medx.com/unlock" class="email-button">Desbloquear Conta</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 11 - Mensagem Direta do Suporte
    public final String SUPPORT_MESSAGE_TEMPLATE = """
        <h2>Mensagem do Suporte {{app_name}}</h2>
        <p>Olá {{nome}},</p>
        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 10px 0;">
            {{mensagem}}
        </div>
        <p><strong>Tipo:</strong> {{tipo_mensagem}}</p>
        <p><strong>Prioridade:</strong> {{prioridade}}</p>
        <a href="https://medx.com/support/ticket/{{ticket_id}}" class="email-button">Responder</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 12 - Confirmação de Saque
    public final String WITHDRAWAL_CONFIRMATION_TEMPLATE = """
        <h2>Saque Processado</h2>
        <p>Olá {{nome}},</p>
        <p>Seu saque de <strong>{{valor}}</strong> {{moeda}} foi enviado para:</p>
        <p><strong>Conta Destino:</strong> {{conta_destino}}</p>
        <p><strong>Código de Rastreio:</strong> {{codigo_rastreio}}</p>
        <p>Data: {{data}}</p>
        <a href="https://medx.com/transactions/{{transaction_id}}" class="email-button">Ver Detalhes</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 13 - Limite de Transação Atingido
    public final String TRANSACTION_LIMIT_REACHED_TEMPLATE = """
        <h2>Limite de Transação Atingido</h2>
        <p>Olá {{nome}},</p>
        <p>Você atingiu o limite de <strong>{{tipo_limite}}</strong> ({{valor_limite}} {{moeda}}).</p>
        <p>Para aumentar seus limites, complete a verificação de conta:</p>
        <a href="{{link_verificacao}}" class="email-button">Completar KYC</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 14 - Confirmação de Pagamento de Serviço
    public final String SERVICE_PAYMENT_CONFIRMATION_TEMPLATE = """
        <h2>Pagamento de Serviço Confirmado</h2>
        <p>Olá {{nome}},</p>
        <p>Seu pagamento para <strong>{{servico}}</strong> foi processado:</p>
        <p><strong>Valor:</strong> {{valor}} {{moeda}}</p>
        <p><strong>Referência:</strong> {{referencia}}</p>
        <p><strong>Data:</strong> {{data}}</p>
        <a href="{{link_comprovante}}" class="email-button">Baixar Comprovante</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 15 - Alerta de Saldo Baixo
    public final String LOW_BALANCE_ALERT_TEMPLATE = """
        <h2>Saldo Baixo na Carteira</h2>
        <p>Olá {{nome}},</p>
        <p>Seu saldo atual de {{saldo_atual}} {{moeda}} está abaixo do limite configurado ({{limite}} {{moeda}}).</p>
        <a href="https://medx.com/deposit" class="email-button">Adicionar Fundos</a>
        <p>Equipe {{app_name}}</p>
        """;

    // 16 - Convite para Grupo/Parceria
    public final String GROUP_INVITATION_TEMPLATE = """
        <h2>Convite para {{grupo_nome}}</h2>
        <p>Olá {{nome}},</p>
        <p>Você foi convidado(a) para participar do grupo <strong>{{grupo_nome}}</strong> no {{app_name}}.</p>
        <p><strong>Privilégios:</strong> {{privilegios}}</p>
        <a href="{{link_aceitar}}" class="email-button" style="background-color: #28a745;">Aceitar Convite</a>
        <a href="{{link_recusar}}" class="email-button" style="background-color: #dc3545;">Recusar</a>
        <p>Equipe {{app_name}}</p>
        """;

    public final String BOOKING_CONFIRMED_TEMPLATE = """
    <h2>Agendamento Confirmado</h2>
    <p>Olá {{nome}},</p>
    <p>Seu agendamento foi confirmado com sucesso:</p>
    <p><strong>Serviço:</strong> {{servico}}<br><strong>Profissional:</strong> {{profissional}}<br><strong>Data:</strong> {{data}} às {{hora}}</p>
    <a href="{{link_detalhes}}" class="email-button">Ver Detalhes</a>
    <p>Equipe {{app_name}}</p>
    """;

    public final String BOOKING_REMINDER_TEMPLATE = """
    <h2>Lembrete de Agendamento</h2>
    <p>Olá {{nome}},</p>
    <p>Este é um lembrete do seu agendamento:</p>
    <p><strong>Serviço:</strong> {{servico}}<br><strong>Profissional:</strong> {{profissional}}<br><strong>Data:</strong> {{data}} às {{hora}}</p>
    <p>Chegue com 10 minutos de antecedência 😉</p>
    <p>Equipe {{app_name}}</p>
    """;

    public final String BOOKING_CANCELLED_TEMPLATE = """
    <h2>Agendamento Cancelado</h2>
    <p>Olá {{nome}},</p>
    <p>Seu agendamento de <strong>{{servico}}</strong> com {{profissional}} em {{data}} às {{hora}} foi cancelado.</p>
    <p>Se não foi você quem cancelou, entre em contato com o suporte.</p>
    <a href="https://agendaki.com/support" class="email-button">Suporte</a>
    <p>Equipe {{app_name}}</p>
    """;

    public final String WORKER_INVITATION_TEMPLATE = """
    <h2>Convite para se juntar ao {{app_name}}</h2>
    <p>Olá {{nome}},</p>
    <p>Você foi convidado(a) para se juntar à equipe de <strong>{{empresa}}</strong> no {{app_name}}.</p>
    <p><strong>Função:</strong> {{cargo}}</p>
    <a href="{{link_aceitar}}" class="email-button" style="background-color: #28a745;">Aceitar Convite</a>
    <a href="{{link_recusar}}" class="email-button" style="background-color: #dc3545;">Recusar</a>
    <p>Equipe {{app_name}}</p>
    """;

    public final String FEEDBACK_REQUEST_TEMPLATE = """
    <h2>Ajude-nos a Melhorar</h2>
    <p>Olá {{nome}},</p>
    <p>Esperamos que tenha gostado do serviço <strong>{{servico}}</strong> realizado em {{data}}.</p>
    <p>Gostaríamos muito de saber sua opinião!</p>
    <a href="{{link_feedback}}" class="email-button">Deixar Avaliação</a>
    <p>Equipe {{app_name}}</p>
    """;

    public final String PROMOTION_TEMPLATE = """
    <h2>Promoção Especial para Você 🎉</h2>
    <p>Olá {{nome}},</p>
    <p>Aproveite nossa promoção exclusiva:</p>
    <p><strong>{{descricao_promocao}}</strong></p>
    <a href="{{link_promocao}}" class="email-button">Agendar Agora</a>
    <p>Equipe {{app_name}}</p>
    """;

    public String buildTemplate(int templateNum, Map<String, String> params) {
        String template = switch (templateNum) {
            case 0 -> WELCOME_TEMPLATE;
            case 1 -> EMAIL_VERIFICATION_TEMPLATE;
            case 2 -> PASSWORD_RESET_OPT_TEMPLATE;
            case 3 -> DEPOSIT_CONFIRMATION_TEMPLATE;
            case 4 -> TRANSFER_RECEIVED_TEMPLATE;
            case 5 -> TRANSFER_SENT_TEMPLATE;
            case 6 -> PASSWORD_CHANGED_TEMPLATE;
            case 7 -> SUSPICIOUS_LOGIN_TEMPLATE;
            case 8 -> FAILED_TRANSACTION_TEMPLATE;
            case 9 -> REVERSED_TRANSACTION_TEMPLATE;
            case 10 -> MULTIPLE_LOGIN_ATTEMPTS_TEMPLATE;
            case 11 -> SUPPORT_MESSAGE_TEMPLATE;
            case 12 -> WITHDRAWAL_CONFIRMATION_TEMPLATE;
            case 13 -> TRANSACTION_LIMIT_REACHED_TEMPLATE;
            case 14 -> SERVICE_PAYMENT_CONFIRMATION_TEMPLATE;
            case 15 -> LOW_BALANCE_ALERT_TEMPLATE;
            case 16 -> GROUP_INVITATION_TEMPLATE;
            case 17 -> ACCOUNT_SUSPENDED_TEMPLATE;
            case 18 -> KYC_APPROVED_TEMPLATE;
            case 19 -> KYC_REJECTED_TEMPLATE;
            case 20 -> KYC_REQUEST_TEMPLATE;
            case 21 -> BOOKING_CONFIRMED_TEMPLATE;
            case 22 -> BOOKING_REMINDER_TEMPLATE;
            case 23 -> BOOKING_CANCELLED_TEMPLATE;
            case 24 -> WORKER_INVITATION_TEMPLATE;
            case 25 -> FEEDBACK_REQUEST_TEMPLATE;
            case 26 -> PROMOTION_TEMPLATE;
            default -> "";
        };

        // Processamento do template base
        String emailContent = DEFAULT_CONTENT
                .replace("{{email_content}}", template)
                .replace("{{app_name}}", params.getOrDefault("app_name", APP_NAME));

        // Substituição de todos os placeholders
        for (Map.Entry<String, String> entry : params.entrySet()) {
            emailContent = emailContent.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }

        return emailContent;

    }
}
