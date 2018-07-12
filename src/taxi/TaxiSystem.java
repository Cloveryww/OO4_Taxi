package taxi;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;


public class TaxiSystem {
	public static TaxiGUI gui=new TaxiGUI();
	public static int TAXINUM=100;
	public static int MAPSIZE=80;
	public static int MAPSIZE2=6400;
	
	/**
	 * @REQUIRES: FILE.exist(filename)&&map!=null&&taxis!=null&&requestList!=null
	 * @MODIFIES: map,taxis,requestList
	 * @EFFECTS: init map taxis requestList according file;
	 */
	public static void load_init(String filename,Map map,Taxi[] taxis,RequestList requestList)//加载测试初始化文件
	{
		File file = new File(filename);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String str = null;
            int tag=1;
            while ((str = reader.readLine()) != null) {
            	str=str.replaceAll("\\s+", "");//去除空白字符
            	if(tag==1) {
            		System.out.println("begin load "+filename+"...\n"+str);
            		tag=0;
            		continue;
            	}
            	if(str.equals(""))//跳过空行
            	{
            		continue;
            	}
            	if(str.equals("#map"))
            	{
            		if(!(str = reader.readLine()).equals("#end_map"))//读取地图
            		{
            			str=str.replaceAll("\\s+", "");//去除空白字符
            			int first=1;
            			for(int i=0;i<MAPSIZE;i++){
            				String[] strArray = null;
            				try{
            					if(first==1)
            					{
            						first=0;
            						strArray=str.split("");
            					}else {
            						str=reader.readLine();
            						str=str.replaceAll("\\s+", "");//去除空白字符
            						strArray=str.split("");
            					}
            				}catch(Exception e){
            					System.out.println("预设文件有误，程序退出1");
            					System.exit(1);
            				}
            				for(int j=0;j<MAPSIZE;j++){
            					try{
            						map.map[i][j]=Integer.parseInt(strArray[j]);
            						if(map.map[i][j]>4||map.map[i][j]<0)
            						{
            							System.out.println("预设文件有误，程序退出2");
            							System.exit(1);
            						}
            					}catch(Exception e){
            						System.out.println("预设文件有误，程序退出3");
            						System.exit(1);
            					}
            				}
            			}
            			if(!(str = reader.readLine()).equals("#end_map"))
            			{
            				System.out.println("预设文件有误，程序退出4");
    						System.exit(1);
            			}
            		}
            		continue;
            	}else if(str.equals("#flow"))
            	{
            		if(!(str = reader.readLine()).equals("#end_flow"))
            		{
            			str=str.replaceAll("\\s+", "");//去除空白字符
            			while(true) {
            				str=str.replaceAll("\\s+", "");//去除空白字符
            				String[] terms=str.split("\\(|\\)|\\,");//(x1,y1),(x2,y2),value
            				int s_x,s_y,d_x,d_y,value;
            				try {
            					s_x = Integer.parseInt(terms[1]);
            					s_y = Integer.parseInt(terms[2]);
            					d_x = Integer.parseInt(terms[5]);
            					d_y = Integer.parseInt(terms[6]);
            					value = Integer.parseInt(terms[8]);
            					map.setflow(new Point(s_x,s_y), new Point(d_x,d_y), value);
            				}catch(Exception e)
            				{
            					System.out.println("预设文件有误，程序退出5");
        						System.exit(1);
            				}
            				if((str = reader.readLine()).equals("#end_flow"))
            				{
            					break;
            				}
            			}
            		}
            		continue;
            	}else if(str.equals("#taxi"))
            	{
            		if(!(str = reader.readLine()).equals("#end_taxi"))
            		{
            			while(true) {
            				str=str.replaceAll("\\s+", "");//去除空白字符
            				String[] terms=str.split("\\(|\\)|\\,");//1,0, 10, (1, 1)
            				//status服务状态取值为0，接单状态取值为1，等待服务取值为2，停止状态取值为3。
            				int no,status,credit,loc_x,loc_y;
            				try {
            					no = Integer.parseInt(terms[0]);
            					status = Integer.parseInt(terms[1]);
            					credit = Integer.parseInt(terms[2]);
            					loc_x = Integer.parseInt(terms[4]);
            					loc_y = Integer.parseInt(terms[5]);
            					taxis[no].setCredit(credit);
            					taxis[no].setLoc(new Point(loc_x,loc_y));
            					if(status==2)
            					{
            						taxis[no].setState(TaxiState.IDLE);
            					}else if(status==3)
            					{
            						taxis[no].setState(TaxiState.STOP);
            					}else
            					{
            						System.out.println("预设文件有误，程序退出:不能凭空将出租车的状态设为服务状态或者接单状态");
            					}
            				}catch(Exception e)
            				{
            					System.out.println("预设文件有误，程序退出6");
        						System.exit(1);
            				}
            				if((str = reader.readLine()).equals("#end_taxi"))
            				{
            					break;
            				}
            			}
            		}
            		continue;
            	}else if(str.equals("#request"))
            	{
            		if(!(str = reader.readLine()).equals("#end_request"))
            		{
            			while(true) {
            				str=str.replaceAll("\\s+", "");//去除空白字符
            				String[] terms=str.split("\\(|\\)|\\,");//[CR, (X1, Y1),(X2, Y2)]
            				int src_x,src_y,dst_x,dst_y;
            				try {
            					src_x = Integer.parseInt(terms[2]);
            					src_y = Integer.parseInt(terms[3]);
            					dst_x = Integer.parseInt(terms[6]);
            					dst_y = Integer.parseInt(terms[7]);
            					Request req = new Request(src_x, src_y, dst_x, dst_y, 0, map, null);
            					requestList.getreqList().add(req);
            					gui.RequestTaxi(req.getSrc(),req.getDst());//更新GUI
            				}catch(Exception e)
            				{
            					System.out.println("预设文件有误，程序退出");
        						System.exit(1);
            				}
            				if((str = reader.readLine()).equals("#end_request"))
            				{
            					return;
            				}
            			}
            		}
            		return;
            	}else
            	{
            		System.out.println("预设文件加载错误，请检查预设文件格式");
            		break;
            	}
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
	}
	
	public static void main(String[] args) {
		Map map=new Map(gui);//建立地图
		map.readmap("map.txt");//在这里设置地图文件路径
		map.updateminDistances();//初始化
		gui.LoadMap(map.map, MAPSIZE);
		Taxi[] taxis = new Taxi[TAXINUM];//出租车
        RequestList requestList = new RequestList();//顾客请求队列
        for (int i=0;i<TAXINUM;i++)//创建出租车线
        {
            taxis[i] = new Taxi(i,map,gui);//创建
        }
        System.out.println("请输入预设测试场景文件的名称:(如test.txt,如果不加载则输入NO,以回车键结束)");
        Scanner input = new Scanner(System.in);  //控制台输入
        String str = input.nextLine();
        if(!str.equals("NO"))//加载预设测试场景文件
        {
        	load_init(str,map,taxis,requestList);
        	gui.LoadMap(map.map, MAPSIZE);
        }
        

		Thread temp0;
        temp0 = new Thread(map);
        temp0.start();//启动地图线程，用于更新道路流量
        for (int i=0;i<TAXINUM;i++)//启动出租车线程
        {
        	Thread temp;
            temp = new Thread(taxis[i]);
            temp.start();//启动调度器
            //启动线程
        }
        Scheduler scheduler = new Scheduler(taxis,requestList,map,gui);
        Thread temp;
        temp = new Thread(scheduler);
        temp.start();//启动调度器
        InputRequest inputRequest = new InputRequest(requestList,gui,map);
        inputRequest.begin();//开始从控制台读取请求
	}

}
