package project1.ares.templates;

import java.util.Map;

public class MessageTemplates {

     public static final String CONVITE_PARA_TRABALHADOR = """
Olá {{name}},\n
Você recebeu uma proposta para trabalhar com {{companyName}}.\n
Clique no link abaixo para visualizar os detalhes e aceitar ou recusar:\n
👉 {{link}}\n
Este convite é válido somente para o número: +258 {{phone_number}} e expira em 48h.
""";

     public static final String LEMBRETE = """
Olá {{name}},\n
Seu convite de trabalho com AgendAki expira em breve!\n
Ainda dá tempo de responder:\n
👉 {{link}}
""";

     public static final String CONVITE_EXPIRADO = """
Este convite expirou ⏳\n
Peça um novo link a {{link}} para continuar.
""";

     public static String buildTemplate(int templateNum, Map<String, String> params) {
         String template = switch (templateNum) {
              case 1 -> CONVITE_PARA_TRABALHADOR;
              case 2 -> CONVITE_EXPIRADO;
              case 3 -> LEMBRETE;
              default -> "AgendaAki: Mensagem padrão";
         };

         // Substituição de todos os placeholders
         for (Map.Entry<String, String> entry : params.entrySet()) {
             template = template.replace("{{" + entry.getKey() + "}}",
                     entry.getValue() != null ? entry.getValue() : "");
         }

         return template;
     }
}
