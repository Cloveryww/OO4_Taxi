package taxi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedWriter;
import java.util.Date;
import java.util.Scanner;

public class AutoTest implements Runnable{

	PipedWriter out;
	Scanner reader;
	AutoTest(PipedWriter out) {  
        this.out = out;  
    }  
	@Override
	public void run() {
		String oneRow;
		String[] strs;
		long starttime = new Date().getTime();
		try {
			reader = new Scanner(new BufferedReader(new FileReader("input.txt")));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			System.out.println("Begin Autotest:");
			while (true) 
			{
				oneRow = reader.nextLine();
				strs = oneRow.split("[=]");
				long time = starttime + Long.valueOf(strs[0])*1000;
				long st;
				while(true)
				{
					st=new Date().getTime();
					if(st>time)
					{
						break;
					}
				}
				if(strs[1].equals("END"))
				{
					System.out.println("END Autotest");
					Thread.sleep(1000*120);
					out.write("end\n");  
					break;
				}
				System.out.println("Autoinput: "+strs[1]);
	            out.write(strs[1]+"\n");  
	        }  
	        } catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}finally {  
	            try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}  
	        }  
		
	}
}
