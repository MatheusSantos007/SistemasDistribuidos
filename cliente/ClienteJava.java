package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClienteJava {
    
    
    private static final String IP_SERVIDOR = "10.85.28.42";
    private static final int PORTA = 8080;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = (args.length > 0) ? args[0] : IP_SERVIDOR;

        // Mostrar o Menu ANTES de conectar
        System.out.println("======================================");
        System.out.println("   BEM-VINDO AO CLIENTE   ");
        System.out.println("======================================");
        System.out.println("Escolha uma das perguntas para o Servidor:");
        System.out.println(" [1] - Qual o horário do Servidor?");
        System.out.println(" [2] - Qual a versão do Sistema?");
        System.out.println(" [3] - Qual o(a) melhor professo(a)r do IFPB Campus Guarabira?");
        System.out.println(" [4] - Qual o Melhor time do Mundo?");
        System.out.println("\n (Pode tentar digitar outra coisa para testar a validação do servidor)");
        System.out.print("Digite a sua opção: ");
        
        String opcaoEscolhida = scanner.nextLine().trim();

        // O cliente envia qualquer coisa, e deixa o Servidor fazer o seu trabalho de rejeitar.

        System.out.println("\n[*] A ligar ao Servidor em " + host + ":" + PORTA + "...");

        // 2. Conexão e Comunicação Imediata
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