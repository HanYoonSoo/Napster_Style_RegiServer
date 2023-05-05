import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    숫자 야구 게임을 위해 존재하는 중앙 서버
 */
public class RegiServer {
    public static List<String[]> onlinePeerQueue;   // Peer의 목록을 리스트로 관리
    public static int userNum;  // Peer를 식별하기 위한 식별 번호
    public static Map<Integer, Integer> userMap;    // 포트 번호와 식별 번호 매핑을 위한 Map 객체
    public static Map<Integer, String> userNameMap; // 포트 번호와 닉네임을 매핑하기 위한 Map 객체

    public static void main(String[] args){
        ServerSocket serverSocket = null;   // 서버 소켓 선언
        Socket socket = null;   // Peer의 접속을 위한 소켓 선언
        onlinePeerQueue = new ArrayList<>();
        userNum = 1;
        userMap = new HashMap<>();
        userNameMap = new HashMap<>();

        try{
            serverSocket = new ServerSocket(9000);  // 9000번 포트로 서버 소켓 열기
            System.out.println("서버가 시작되었습니다.");

            /*
                무한 반복되며 접속한 Peer에 대한 Thread를 실행
             */
            while(true){
                socket = serverSocket.accept(); // Peer 접속

                // 접속한 Peer를 관리하기 위해 Queue에 IP 주소와 포트 번호 저장
                onlinePeerQueue.add(new String[]{socket.getInetAddress().getHostAddress(), String.valueOf(socket.getPort())});
                userMap.put(socket.getPort(), userNum++);   // 포트 번호에 따른 식별 번호 설정

                // 각 Peer와의 송수신을 위한 스레드 선언 및 실행
                EchoThread echoThread = new EchoThread(socket);

                echoThread.start();
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            try{
                socket.close();
            }
            catch (IOException e){
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

/*
    각 Peer들과 여러 작업을 진행하는 스레드 클래스
 */
class EchoThread extends Thread{

    Socket socket;

    /*
        Peer와 대화를 나누기 위한 BufferedReader 및 PrintWriter 선언
     */
    InputStream is = null;
    BufferedReader br = null;

    OutputStream os = null;
    PrintWriter pw = null;

    public EchoThread(){
    }

    /*
        RegiServer로부터 Socket 정보를 받아와 관련 객체를 할당
     */
    public EchoThread(Socket socket){
        this.socket = socket;
        try{
            is = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            os = socket.getOutputStream();
            pw = new PrintWriter(os);

        }
        catch(IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /*
        EchoThread가 start되면 실행되는 메소드
     */
    public void run(){
        boolean compare = false;
        try{
            int ch;

            /*
                사용자의 커맨드를 구분하기 위한 반복문
                무한 반복
             */
            while(!compare){
                String command = "";
                while(true) {
                    ch = br.read();
                    if (ch < 0 || ch == '\r')
                        break;
                    command += (char)ch;
                }

                /*
                    입력받은 command에 따른 작업을 수행하기 위한 switch문
                 */
                switch(command){

                    /*
                        help 커맨드를 받은 경우 help 메소드를 통해 명령어 정보 출력
                     */
                    case "help":
                        help();
                        continue;

                    /*
                        현재 온라인인 유저의 목록을 출력하는 메소드 호출
                     */
                    case "online_users":
                        online_users();
                        break;

                    /*
                        유저가 로그오프 하였을 때 유저가 로그오프 되었음을 출력
                     */
                    case "logoff":
                        compare = true;
                        String port = String.valueOf(socket.getPort());
                        for(int i = 0; i < RegiServer.onlinePeerQueue.size(); i++){
                            if(port.equals(RegiServer.onlinePeerQueue.get(i)[1])){  // 로그오프한 Peer의 정보를 queue에서 삭제
                                // 로그오프를 하려는 유저의 닉네임을 출력한 뒤 Queue에서 삭제
                                System.out.println(RegiServer.userNameMap.get(RegiServer.userMap.get(Integer.parseInt(port))) + "유저가 로그아웃 합니다. IP: " + RegiServer.onlinePeerQueue.get(i)[0] + ", Port: " + RegiServer.onlinePeerQueue.get(i)[1]);
                                RegiServer.onlinePeerQueue.remove(i);
                                break;
                            }
                        }

                        /*
                            로그오프한 Peer의 정보를 userMap에서 삭제
                         */
                        for(Integer key: RegiServer.userMap.keySet()){
                            if(key == Integer.parseInt(port)){
                                RegiServer.userMap.remove(key);
                                break;
                            }
                        }
                        close();
                        break;
                    default:
                        /*
                            Peer의 닉네임 정보를 userNameMap에 저장
                         */
                        if(!RegiServer.userNameMap.containsKey(RegiServer.userNum - 1)) {
                            RegiServer.userNameMap.put(RegiServer.userNum - 1, command);
                            System.out.println(command + "유저가 서버에 접속했습니다.");

                            // 접속한 Peer의 IP와 Port 번호를 출력
                            System.out.println("유저의 IP, Port 정보입니다 - IP: " + socket.getInetAddress().getHostAddress() + ", Port: " + socket.getPort());
                        }
                }

            }
        }
        catch(IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally{
            close();
            currentThread().interrupt();
        }
    }

    /*
        예외나 프로그램 종료시 관련 객체들을 close하기 위한 메소드
     */
    public void close(){
        try{
            is.close();
            br.close();
            os.close();
            pw.close();
            socket.close();
            currentThread().interrupt();
        }
        catch(IOException e){
            System.out.println("로그아웃 에러");
            e.printStackTrace();
        }
    }

    /*
        명령어 정보를 출력하기 위한 메소드
     */
    public void help(){
        pw.write("help: 사용할 수 있는 명령어의 목록을 볼 수 있습니다.\n");
        pw.write("online_users: 온라인 유저 목록을 확인할 수 있습니다.\n");
        pw.write("connect: connect [ip] [port] 형태로 다른 유저와 게임을 진행할 수 있습니다.\n");
        pw.write("disconnect: disconnect [peer] 형태로 온라인인 유저와의 연결을 끊습니다.(peer은 유저의 닉네임을 칭합니다.)\n");
        pw.write("guess: guess [peer] [your guessing number] 형태로 연결되어 있는 유저의 숫자를 추측합니다.(peer은 유저의 닉네임을 칭합니다.)\n");
        pw.write("answer: answer [peer] [answer to the guess] 형태로 추측된 숫자에 대한 응답을 보내줍니다.(peer은 유저의 닉네임을 칭합니다.)\n");
        pw.write("logoff: 서버와 로그아웃을 진행합니다.\r\n");

        pw.flush();

    }

    /*
        온라인 유저 정보를 출력하기 위한 메소드
     */
    public void online_users(){
        pw.println("온라인 상태의 유저 목록입니다.");
        for(int i = 0; i < RegiServer.onlinePeerQueue.size(); i++){
            int port = Integer.parseInt(RegiServer.onlinePeerQueue.get(i)[1]);

            // 유저의 식별 번호, 유저의 닉네임, 유저의 IP, 유저의 서버 포트를 출력
            String str =  RegiServer.userMap.get(port) + ". " + RegiServer.userNameMap.get(RegiServer.userMap.get(port)) + ": IP - " + RegiServer.onlinePeerQueue.get(i)[0] + ", port - " + 9001;
            pw.write(str + "\n");
        }
        pw.write("\r");
        pw.flush();

    }
}

