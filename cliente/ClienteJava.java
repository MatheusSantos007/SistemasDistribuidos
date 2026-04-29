package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClienteJava {
    
    // 1. Variáveis de instância (sem 'static')
    private final String IP_SERVIDOR_PADRAO = "10.33.226.42";
    private final int PORTA = 8080;

    // 2. O main apenas cria o objeto e inicia o processo
    public static void main(String[] args) {
        ClienteJava cliente = new ClienteJava();
        cliente.iniciar(args);
    }

    // 3. Toda a lógica vem para um método pertencente ao objeto
    public void iniciar(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Pega o IP dos argumentos ou usa o padrão da instância
        String host = (args.length > 0) ? args[0] : this.IP_SERVIDOR_PADRAO;

        // Mostrar o Menu ANTES de conectar
        System.out.println("======================================");
        System.out.println("   BEM-VINDO AO CLIENTE   ");
        System.out.println("======================================");
        System.out.println("Escolha uma das perguntas para o Servidor:");
        System.out.println(" [1] - Qual o horário do Servidor?");
        System.out.println(" [2] - Qual a versão do Sistema?");
        System.out.println(" [3] - Qual o(a) melhor professor(a) do IFPB Campus Guarabira?");
        System.out.println(" [4] - Qual o Melhor time do Mundo?");
        System.out.println("\n (Pode tentar digitar outra coisa para testar a validação do servidor)");
        System.out.print("Digite a sua opção: ");
        
        String opcaoEscolhida = scanner.nextLine().trim();

        System.out.println("\n[*] A ligar ao Servidor em " + host + ":" + PORTA + "...");

        // Conexão e Comunicação Imediata
        try (Socket socket = new Socket(host, PORTA);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[*] Ligação estabelecida! A enviar a mensagem: [" + opcaoEscolhida + "]");
            
            // Envia a opção imediatamente
            saida.println(opcaoEscolhida);

            System.out.println("[*] A aguardar a resposta da Fila (FIFO) do Servidor...");
            
            // Bloqueio Síncrono final a aguardar a resposta
            String resposta = entrada.readLine();

            if ("ERRO_IP".equals(resposta)) {
                System.out.println("\n[ERRO DE SEGURANÇA] O servidor rejeitou a ligação!");
                System.out.println("Este IP já consumiu a sua vaga. O teste exige máquinas diferentes.");
            } else if (resposta != null) {
                System.out.println("\n===============================================");
                System.out.println("  RESPOSTA DO SERVIDOR: " + resposta);
                System.out.println("===============================================\n");
            } else {
                System.out.println("[ERRO] O servidor encerrou a ligação inesperadamente.");
            }

        } catch (Exception e) {
            System.err.println("[ERRO REDE/FATAL] Não foi possível ligar.");
            System.err.println("Verifique se o Servidor está a correr e se o IP " + host + " está correto.");
        } finally {
            scanner.close();
            System.out.println("[*] Sistema cliente encerrado.");
        }
    }
}