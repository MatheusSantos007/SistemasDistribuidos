package Servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServidorJava {

    // ==========================================
    // CONFIGURAÇÕES DA ARQUITETURA (Agora como atributos da instância)
    // ==========================================
    private final int PORTA = 8080;
    private final int MAX_CLIENTES = 5;

    // Estruturas de Concorrência
    // 1. Fila FIFO 100% Thread-Safe
    private final BlockingQueue<Requisicao> filaProcessamento = new LinkedBlockingQueue<>();
    
    // 2. Controlo de IPs com Set Sincronizado
    private final Set<String> ipsConectados = Collections.synchronizedSet(new HashSet<>());

    // Classe interna para encapsular os dados da fila.
    // Mantém-se 'static' pois funciona apenas como uma estrutura de dados burra (DTO)
    // que não precisa aceder aos métodos/variáveis da classe externa (ServidorJava).
    static class Requisicao {
        Socket socket;
        String ip;
        String opcao;

        public Requisicao(Socket socket, String ip, String opcao) {
            this.socket = socket;
            this.ip = ip;
            this.opcao = opcao;
        }
    }

    // ==========================================
    // PONTO DE ENTRADA (Único método estático)
    // ==========================================
    public static void main(String[] args) {
        // Instanciação: Criamos um objeto do servidor em vez de usar variáveis globais.
        // Assim, aplicamos corretamente a Orientação a Objetos.
        ServidorJava servidor = new ServidorJava();
        servidor.iniciar();
    }

    // ==========================================
    // LÓGICA PRINCIPAL (
    // ==========================================
    public void iniciar() {
        System.out.println("=== SERVIDOR JAVA INICIADO ===");
        System.out.println("[*] A escutar na porta " + PORTA + " (LAN/0.0.0.0)");

        // Inicia a Thread Consumidora (FIFO) referenciando o método da instância (this::processadorFifo)
        Thread threadFifo = new Thread(this::processadorFifo);
        threadFifo.setDaemon(true);
        threadFifo.start();

        try (ServerSocket serverSocket = new ServerSocket(PORTA, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("[*] Servidor pronto. A aceitar até " + MAX_CLIENTES + " ligações...\n");

            int clientesAtendidos = 0;

            while (clientesAtendidos < MAX_CLIENTES) {
                Socket conexao = serverSocket.accept();
                String ipCliente = conexao.getInetAddress().getHostAddress();

                // Validação de IP para bloquear testes da mesma máquina
                if (ipsConectados.contains(ipCliente)) {
                    System.out.println("[SEGURANÇA] Ligação duplicada rejeitada do IP " + ipCliente);
                    PrintWriter saidaErro = new PrintWriter(conexao.getOutputStream(), true);
                    saidaErro.println("ERRO_IP");
                    conexao.close();
                    continue; // Ignora e volta a aguardar
                }

                ipsConectados.add(ipCliente);
                clientesAtendidos++;
                
                System.out.println("[NOVA LIGAÇÃO] Cliente " + clientesAtendidos + "/" + MAX_CLIENTES + " ligado (IP: " + ipCliente + ")");

                // Dispara Thread Produtora para este cliente referenciando o método da instância
                new Thread(() -> this.recepcionistaClientes(conexao, ipCliente)).start();
            }

            System.out.println("\n[SISTEMA] Lotação máxima atingida (5 clientes distintos)!");

            // Evita que o servidor feche antes da fila ser esvaziada
            while (!filaProcessamento.isEmpty()) {
                Thread.sleep(1000);
            }
            // Pequeno delay final para garantir que as últimas respostas viajam pela rede
            Thread.sleep(2000); 
            System.out.println("=== TODAS AS REQUISIÇÕES ATENDIDAS. A ENCERRAR O SISTEMA ===");

        } catch (Exception e) {
            System.err.println("[ERRO FATAL NO SERVIDOR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // THREAD PRODUTORA (Por Cliente) 
    // ==========================================
    private void recepcionistaClientes(Socket conexao, String ip) {
        try {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(conexao.getInputStream()));

            // Lê o NÚMERO ou TEXTO que o cliente enviou
            String opcao = entrada.readLine();
            if (opcao != null) {
                System.out.println("[REDE] Mensagem recebida do IP " + ip + " -> Opção/Texto: " + opcao);
                // Coloca na Fila (Queue) de forma 100% segura
                filaProcessamento.put(new Requisicao(conexao, ip, opcao));
            } else {
                conexao.close();
            }

        } catch (Exception e) {
            System.err.println("[ERRO] Falha com o cliente " + ip + ": " + e.getMessage());
        }
    }

    // ==========================================
    // THREAD CONSUMIDORA (Única) - Agora sem 'static'
    // ==========================================
    private void processadorFifo() {
        System.out.println("[SISTEMA] Thread Processadora (FIFO) iniciada. A aguardar dados...");
        while (true) {
            try {
                // Bloqueia até haver um item na fila
                Requisicao req = filaProcessamento.take();
                
                String resposta;
                // Lógica robusta do Menu: Trata o que conhece, rejeita o que desconhece
                switch (req.opcao.trim()) {
                    case "1":
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                        resposta = "Hora do Servidor: " + dtf.format(LocalDateTime.now());
                        break;
                    case "2":
                        resposta = "Versao: Servidor Distribuido LAN v2.0 (Java Edition).";
                        break;
                    case "3":
                        resposta = "Status Lotação: Servidor aceita 5 conexoes no total.";
                        break;
                    case "4":
                        resposta = "Uptime: 48 horas online sem interrupcoes.";
                        break;
                    default:
                        // O SERVIDOR REJEITA COMANDOS DESCONHECIDOS AQUI
                        resposta = "ERRO: Comando [" + req.opcao + "] desconhecido. O servidor so entende comandos de 1 a 4.";
                }

                // Atraso didático para evidenciar o processamento sequencial no terminal
                Thread.sleep(500);

                PrintWriter saida = new PrintWriter(req.socket.getOutputStream(), true);
                saida.println(resposta);
                System.out.println("[PROCESSAMENTO FIFO] Resposta enviada para o IP " + req.ip + " -> " + resposta);

                // Fecha a ligação após responder
                req.socket.close();

            } catch (InterruptedException e) {
                break; // Se a thread for interrompida, encerra silenciosamente
            } catch (Exception e) {
                System.err.println("[ERRO NO PROCESSAMENTO] " + e.getMessage());
            }
        }
    }
}