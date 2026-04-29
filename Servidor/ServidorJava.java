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

    
    // CONFIGURAÇÕES DA ARQUITETURA
    
    private final int PORTA = 8080;
    private final int MAX_SIMULTANEAS = 5;

    // Fila FIFO 100% Thread-Safe
    private final BlockingQueue<Requisicao> filaProcessamento = new LinkedBlockingQueue<>(MAX_SIMULTANEAS);
    
    // Controle de IPs
    private final Set<String> ipsAtivos = Collections.synchronizedSet(new HashSet<>());

    // Classe interna para encapsular os dados da fila
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

    public static void main(String[] args) {
        ServidorJava servidor = new ServidorJava();
        servidor.iniciar();
    }

    public void iniciar() {
        System.out.println("=== SERVIDOR JAVA INICIADO ===");
        System.out.println("[*] A escutar na porta " + PORTA + " (LAN/0.0.0.0)");

        // 1. Inicia 5 Threads Consumidoras. 
        for (int i = 0; i < MAX_SIMULTANEAS; i++) {
            Thread threadConsumidora = new Thread(this::processadorFifo);
            threadConsumidora.setDaemon(true);
            threadConsumidora.start();
        }

        try (ServerSocket serverSocket = new ServerSocket(PORTA, 50, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("[*] Servidor pronto. Trabalhadores ativos: " + MAX_SIMULTANEAS + "\n");

            // Loop infinito: o servidor nunca se desliga sozinho
            while (true) {
                Socket conexao = serverSocket.accept();
                String ipCliente = conexao.getInetAddress().getHostAddress();

                
                if (ipsAtivos.contains(ipCliente)) {
                    System.out.println("[SEGURANÇA] Ligação simultânea rejeitada do IP " + ipCliente);
                    PrintWriter saidaErro = new PrintWriter(conexao.getOutputStream(), true);
                    saidaErro.println("ERRO_IP");
                    conexao.close();
                    continue; 
                }

                // Adiciona o IP à lista de bloqueio temporário
                ipsAtivos.add(ipCliente);
                System.out.println("[NOVA LIGAÇÃO] Cliente ligado (IP: " + ipCliente + ")");

                // Dispara Thread para ler a mensagem do cliente
                new Thread(() -> this.recepcionistaClientes(conexao, ipCliente)).start();
            }

        } catch (Exception e) {
            System.err.println("[ERRO FATAL NO SERVIDOR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    // THREAD PRODUTORA 
    
    private void recepcionistaClientes(Socket conexao, String ip) {
        try {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(conexao.getInputStream()));

            String opcao = entrada.readLine();
            if (opcao != null) {
                System.out.println("[REDE] Mensagem recebida do IP " + ip + " -> Opção: " + opcao);
                // Coloca na fila. Se a fila estiver cheia (5 elementos), aguarda automaticamente.
                filaProcessamento.put(new Requisicao(conexao, ip, opcao));
            } else {
                ipsAtivos.remove(ip); // Liberta o IP se o cliente enviou mensagem nula
                conexao.close();
            }

        } catch (Exception e) {
            ipsAtivos.remove(ip); // Liberta o IP em caso de erro na leitura
            System.err.println("[ERRO] Falha com o cliente " + ip + ": " + e.getMessage());
        }
    }

    
    // THREAD CONSUMIDORA 
    
    private void processadorFifo() {
        while (true) {
            Requisicao req = null;
            try {
                // Fica a aguardar que chegue um pedido à fila
                req = filaProcessamento.take();
                
                String resposta;
                switch (req.opcao.trim()) {
                    case "1":
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                        resposta = "Hora do Servidor: " + dtf.format(LocalDateTime.now());
                        break;
                    case "2":
                        resposta = "Versao: Servidor Distribuido LAN v2.0 (Java Edition).";
                        break;
                    case "3":
                        resposta = "Status Lotacao: Servidor a operar com limite de 5 sessoes simultaneas.";
                        break;
                    case "4":
                        resposta = "Uptime: Servidor estavel e robusto.";
                        break;
                    default:
                        resposta = "ERRO: Comando [" + req.opcao + "] desconhecido.";
                }

                // Atraso de 2 segundos
                
                Thread.sleep(2000);

                PrintWriter saida = new PrintWriter(req.socket.getOutputStream(), true);
                saida.println(resposta);
                System.out.println("[PROCESSAMENTO] Resposta enviada para o IP " + req.ip + " -> " + resposta);

            } catch (InterruptedException e) {
                break; 
            } catch (Exception e) {
                System.err.println("[ERRO NO PROCESSAMENTO] " + e.getMessage());
            } finally {
                // 3. Libertação da Vaga 
                if (req != null) {
                    ipsAtivos.remove(req.ip);
                    try {
                        req.socket.close();
                    } catch (Exception ignore) {}
                }
            }
        }
    }
}