package taxi;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InputRequest {
	private long start_time;
	private RequestList requestList;
	private TaxiGUI taxiGUI;
	private Map map;
	private static PrintWriter printer;
	
	
	InputRequest(RequestList requestList,TaxiGUI taxiGUI,Map map)
	{
		this.requestList = requestList;
		this.taxiGUI = taxiGUI;
		this.map = map;
		long t=System.currentTimeMillis();
		this.start_time=t-t%100;
		//System.out.println("InputRequest start_time="+start_time);
	}
	/**
	 * @REQUIRES:(curTime>=0)&&str match "^\\[CR,\\(\\+?\\d+,\\+?\\d+\\),\\(\\+?\\d+,\\+?\\d+\\)\\]$";
	 * @MODIFIES:None;
	 * @EFFECTS:(\result.getSrc.getX() >=0 && \result.getSrc.getX() <80)&&
	 * 			(\result.getDst.getX() >=0 && \result.getDst.getX() <80)&& 		
	 * 			\result.getReqTime()==curTime;				
	 */ 
	Request string2Req(String str,long curTime)
	{
		Request req=null;
		String[] terms=str.split("\\(|\\)|\\,");
		int len=terms.length;
		if(len==9)//[CR,(2,3),(10,12)]
		{
			try {
				int src_x,src_y,dst_x,dst_y;
				src_x = Integer.parseInt(terms[2]);
				src_y = Integer.parseInt(terms[3]);
				dst_x = Integer.parseInt(terms[6]);
				dst_y = Integer.parseInt(terms[7]);
				if(((src_x==dst_x)&&(src_y==dst_y))||src_x<0||src_x>=TaxiSystem.MAPSIZE||src_y<0||src_y>=TaxiSystem.MAPSIZE||dst_x<0||dst_x>=TaxiSystem.MAPSIZE||dst_y<0||dst_y>=TaxiSystem.MAPSIZE)
				{
					return null;
				}else
				{//(long)(((int)(curTime/100))*100)
					req = new Request(src_x,src_y,dst_x,dst_y,curTime-(curTime%100),map,printer);
					return req;
				}
			} catch (Exception e) {
			    return null;
			}
		}else//出错了
		{
			return null;
		}
	}
	
	/**
	 * @REQUIRES:requestList!=null;
	 * @MODIFIES:requestList;
	 * @EFFECTS:read each valid request from console or file;			
	 */ 
	void begin()
	{
		try {
			printer = new PrintWriter(new BufferedWriter(new FileWriter("log.txt")));
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		//为预先加载的请求添加printer
		for(Request req:requestList.getreqList())
		{
			req.setPrinter(printer);
		}
		
		
		int inputtype=1;
		Scanner input;
		PipedWriter out = new PipedWriter();  
        PipedReader in = new PipedReader();  
        try {
			out.connect(in);
		} catch (IOException e1) {
			e1.printStackTrace();
		}  
		if(inputtype==0)//自动测试
		{
			AutoTest autotest = new AutoTest(out);
			new Thread(autotest).start();
			input= new Scanner(in);
		}
		else{
			input = new Scanner(System.in);  //控制台输入
		}
        Pattern pattern = Pattern.compile("^\\[CR,\\(\\+?\\d+,\\+?\\d+\\),\\(\\+?\\d+,\\+?\\d+\\)\\]$");
        try {
            while (true) {
                String str = input.nextLine();
                if (str.equals("end")) 
                {
                	break;//结束退出
                }
                String str_no_space = str.replaceAll("\\s+", "");//去除空白字符
                try {
                if(str.startsWith("[OPEN"))//打开或关闭道路   [OPEN,(10,10),(10,11)]
                {
                	String[] terms=str.split("\\(|\\)|\\,");
                	if(terms.length==9)
                	{
                		map.openOneRoad(Integer.parseInt(terms[2]), Integer.parseInt(terms[3]), Integer.parseInt(terms[6]), Integer.parseInt(terms[7]));
                	}else
                	{
                		System.out.println("Order invalid");
                	}
                	continue;
                }else if(str.startsWith("[CLOSE"))
                {
                	String[] terms=str.split("\\(|\\)|\\,");
                	if(terms.length==9)
                	{
                		map.closeOneRoad(Integer.parseInt(terms[2]), Integer.parseInt(terms[3]), Integer.parseInt(terms[6]), Integer.parseInt(terms[7]));
                	}else
                	{
                		System.out.println("Order invalid");
                	}
                	continue;
                }
                }catch(Exception e)
                {
                	System.out.println("Order invalid");
                }
                long curTime = System.currentTimeMillis()-start_time;
                Matcher m0=pattern.matcher(str_no_space);
            	if(!m0.matches())
            	{
            		System.out.println("Request invalid "+str);
            		continue;
            	}
            	Request curreq = string2Req(str_no_space,curTime);//S
            	if(curreq==null)//请求格式不合法
            	{
            		System.out.println("Request invalid "+str);
            		continue;
            	}else//请求格式合法
            	{
            		if(requestList.contain(curreq))
            		{
            			System.out.println("Request same "+str);
            			continue;
            		}else//完全合法的一个新请求
            		{
            			requestList.offer(curreq);//添加到请求队列中
            			taxiGUI.RequestTaxi(curreq.getSrc(),curreq.getDst());//更新GUI
            		}
            	}
            }
            System.exit(0);//退出
        }catch (Exception e) {
            System.out.println("Input Request error!");
            input.close();
            System.exit(0);
        }
		
		
		
		
		
	}
	
	
	
	
	
	
	

}
