package SP18_simulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



/**
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다.
 * 크게 네가지의 가상 자원 공간을 선언하고, 이를 관리할 수 있는 함수들을 제공한다.<br><br>
 * 
 * 1) 입출력을 위한 외부 장치 또는 device<br>
 * 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.<br>
 * 3) 연산을 수행하는데 사용하는 레지스터 공간.<br>
 * 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들. 
 * <br><br>
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면,
 * 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서 차이가 있다.
 */
public class ResourceManager{
	/**
	 * deviceManager는  디바이스의 이름을 입력받았을 때 해당 디바이스의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
	 * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을 넣음으로서 이를 관리할 수 있다.
	 * <br><br>
	 * 변형된 형태로 사용하는 것 역시 허용한다.<br>
	 * 예를 들면 key값으로 String대신 Integer를 사용할 수 있다.
	 * 파일 입출력을 위해 사용하는 stream 역시 자유로이 선택, 구현한다.
	 * <br><br>
	 * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
	 */
	HashMap<String,Object> deviceManager = new HashMap<String,Object>();
	char[] memory = new char[65536]; 
	int[] register = new int[10];
	double register_F;
	
	String nowDev;
	List<String> progName;  //각 프로그램의 이름
	List<Integer> progLength;  //각 프로그램의 길이
	List<Integer> progAddr;  //각 프로그램의 주소

	SymbolTable symtabList;  //ESTAB로 사용
	
	ResourceManager() {
		symtabList = new SymbolTable();
		progName = new ArrayList<String>();
		progLength = new ArrayList<Integer>();
		progAddr = new ArrayList<Integer>();
	}
	
	

	/**
	 * 메모리, 레지스터등 가상 리소스들을 초기화한다.
	 */
	public void initializeResource(){    
		for(int i=0;i< memory.length;i++)
			memory[i]=0;
		
		for(int i=0;i< register.length;i++)
			register[i]=0;
		register_F=0;
	}
	
	/**
	 * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할.
	 * 프로그램을 종료하거나 연결을 끊을 때 호출한다.
	 * @throws IOException 
	 */
	public void closeDevice() throws IOException {
		BufferedReader in = (BufferedReader) deviceManager.get("F1");
		in.close();
		
		BufferedWriter out = (BufferedWriter) deviceManager.get("05");
		out.flush();
		out.close();
	}
		
	
	/**
	 * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수.
	 * 입출력 stream을 열고 deviceManager를 통해 관리시킨다.
	 * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
	 */
	public void testDevice(String devName) {
		BufferedReader bufReader =null;
		BufferedWriter bufWriter = null;

		if(devName.equals("F1")&&deviceManager.get("F1")==null) {  //F1 디바이스가 이미 들어가있으면 패스하고, 아니면 넣는다.
			FileReader filereader = null;
			try {
				filereader = new FileReader(devName+".txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			bufReader = new BufferedReader(filereader);
			if(bufReader!=null) deviceManager.put(devName, bufReader);
		}
		else if(devName.equals("05")&&deviceManager.get("05")==null) {  ////05 디바이스가 이미 들어가있으면 패스하고, 아니면 넣는다.
			Writer filewriter = null;
			try {
				filewriter = new FileWriter(new File(devName+".txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			bufWriter = new BufferedWriter(filewriter);
			if(bufWriter!=null) deviceManager.put(devName, bufWriter);
		}
		setRegister(9,-1);   //sw register을 less상태로 바꿔 RD,WD를 가능하게 한다.
	}

	/**
	 * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
	 * @param devName 디바이스의 이름
	 * @param num 가져오는 글자의 개수
	 * @return 가져온 데이터
	 */
	public char readDevice(String devName, int num){   //RD명령어는 한 글자씩밖에 못읽어들이므로 return을 char로 하기
		int read = 0;
		try {
			BufferedReader buf = (BufferedReader) deviceManager.get(devName);
			if(buf!=null)
			read = buf.read();
			nowDev=devName;
		}catch(IOException er) {
			er.printStackTrace();
		}
		if(read==-1) nowDev = "";  //파일의 끝에 도달할 경우 simulator의 사용중인 장치 항목을 비우기
		return (char)read;
	}

	/**
	 * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
	 * @param devName 디바이스의 이름
	 * @param data 보내는 데이터
	 * @param num 보내는 글자의 개수
	 */
	public void writeDevice(String devName, char data, int num){  //이것도 WD명령어는 한 글자씩밖에 못읽어들이므로 return을 char로 하기
		try {
			BufferedWriter buf = (BufferedWriter) deviceManager.get(devName);
			buf.write(data);
			nowDev=devName;
		}catch(IOException e) {
			e.printStackTrace();
		}
		if(register[5]-1==register[1]) nowDev=""; //맨 마지막 글자에 도달하면 simulator의 사용중인 장치 항목을 비우기
	}
	
	/**
	 * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
	 * @param location 메모리 접근 위치 인덱스
	 * @param num 데이터 개수
	 * @return 가져오는 데이터
	 */
	public char[] getMemory(int location, int num){
		int rIndex=0;
		char[] res = new char[num];
		for(int i=location;i<location+num;i++) {
			res[rIndex++] = memory[i];
		}
		return res;
	}

	/**
	 * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다. 
	 * @param locate 접근 위치 인덱스
	 * @param data 저장하려는 데이터
	 * @param num 저장하는 데이터의 개수
	 */
	public void setMemory(int locate, char[] data, int num){
		int iIndex = 0;
		for(int i=locate;i<locate+num;i++)
			memory[i]=data[iIndex++];
	}

	/**
	 * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * @param regNum 레지스터 분류번호
	 * @return 레지스터가 소지한 값
	 */
	public int getRegister(int regNum){
		return register[regNum];
	}

	/**
	 * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * @param regNum 레지스터의 분류번호
	 * @param value 레지스터에 집어넣는 값
	 */
	public void setRegister(int regNum, int value){
		register[regNum] = value;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data){
		char[] temp = new char[3]; 
 		for(int i = 2; i > 0; i--){ 
 			temp[i] = (char)(data & 0xFF); 
	 		data = data >> 8; 
	 	} 
 		temp[0] = (char)(data & 0xFF); 
		return temp; 

	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
	 * @param data
	 * @return
	 */
	public int byteToInt(char[] data){   //주석에 char[]값을 int형태로 변환한다고 되어 있어 
											//인자의 byte[]를 char[]로 바꾼다.
		int num = data.length;
		int temp = 0;
		for(int i=0;i<num-1;i++) {
			temp += data[i];
			temp = temp << 8;
		}
		temp += data[num-1];
		return temp;
	}
}