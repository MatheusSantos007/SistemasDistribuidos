import socket
import sys


# CONFIGURAÇÕES DO CLIENTE

IP_SERVIDOR = "10.85.28.42"  
PORTA = 8080

def main():
    # 1. Pega o IP dos argumentos ou usa o padrão (igual ao args[0] do Java)
    host = sys.argv[1] if len(sys.argv) > 1 else IP_SERVIDOR

    # 2. Mostrar o Menu ANTES de conectar
    print("======================================")
    print("   BEM-VINDO AO CLIENTE   ")
    print("======================================")
    print("Escolha uma das perguntas para o Servidor:")
    print(" [1] - Qual o horário do Servidor?")
    print(" [2] - Qual a versão do Sistema?")
    print(" [3] - Qual o status de Lotação?")
    print(" [4] - Qual o uptime do Servidor?")
    print("\n (Pode tentar digitar outra coisa para testar a validação do servidor)")
    
    opcao_escolhida = input("Digite a sua opção: ").strip()

    print(f"\n[*] A ligar ao Servidor em {host}:{PORTA}...")

    # 3. Conexão e Comunicação Imediata
    try:
        # Cria o socket TCP (IPv4, Stream)
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(10.0) # Timeout de segurança
            
            # Conecta ao servidor
            s.connect((host, PORTA))
            print(f"[*] Ligação estabelecida! A enviar a mensagem: [{opcao_escolhida}]")
            
            # O Java lê usando readLine()
            mensagem_formatada = opcao_escolhida + "\n"
            s.sendall(mensagem_formatada.encode('utf-8'))

            print("[*] A aguardar a resposta da Fila (FIFO) do Servidor...")
            
            # Bloqueio Síncrono aguardando a resposta (lê até 1024 bytes)
            dados = s.recv(1024)
            resposta = dados.decode('utf-8').strip()

            # 4. Exibição da Resposta
            if resposta == "ERRO_IP":
                print("\n[ERRO DE SEGURANÇA] O servidor rejeitou a ligação!")
                print("Este IP já consumiu a sua vaga. O teste exige máquinas diferentes.")
            elif resposta:
                print("\n===============================================")
                print(f"  RESPOSTA DO SERVIDOR: {resposta}")
                print("===============================================\n")
            else:
                print("[ERRO] O servidor encerrou a ligação inesperadamente.")

    except ConnectionRefusedError:
        print("\n[ERRO REDE/FATAL] Não foi possível ligar.")
        print(f"Verifique se o Servidor Java está a correr e se o IP {host} está correto.")
    except Exception as e:
        print(f"\n[ERRO FATAL NO CLIENTE] {e}")
    finally:
        print("[*] Sistema cliente encerrado.")

if __name__ == "__main__":
    main()