package taxi;


import java.awt.Point;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;


public class Request {
	private Point src;
	private Point dst;
	private long reqTime;
	private Map map;
	private Set<Taxi> candidate_taxis;//存放可能分配的出租车
	public String outputInfo="=====================================\r\n";
	private PrintWriter printer;

	Request(int src_x,int src_y,int dst_x,int dst_y,long reqTime,Map map,PrintWriter printer)
	{
		this.src=new Point(src_x,src_y);
		this.dst=new Point(dst_x,dst_y);
		this.reqTime=reqTime;
		this.map = map;
		this.printer = printer;
		this.candidate_taxis = new LinkedHashSet<Taxi>();
		this.outputInfo+="发出时刻: "+reqTime+"ms\r\n出发地: ("+src_x+","+src_y+")\r\n目的地: ("+dst_x+","+dst_y+")\r\n";
	}
	
	@Override
	public String toString() {
		return "Request [src=(" +(int)src.getX()+","+(int)src.getY()  + "), dst=(" + (int)dst.getX()+","+(int)dst.getY() + ")]";
	}
	
	public void setPrinter(PrintWriter printer) {
		this.printer = printer;
	}

	public Point getSrc() {
		return src;
	}
	public void setSrc(Point src) {
		this.src = src;
	}
	public Point getDst() {
		return dst;
	}
	public void setDst(Point dst) {
		this.dst = dst;
	}
	public long getReqTime() {
		return reqTime;
	}
	public void setReqTime(long reqTime) {
		this.reqTime = reqTime;
	}
	
	
	/**
	 * @REQUIRES:(this.candidate_taxis!=null)&&(t!=null);
	 * @MODIFIES:this.candidate_taxis;
	 * @EFFECTS:this.candidate_taxis.add(t);		
	 */ 
	public void add_candidate_taxi(Taxi t)
	{
		candidate_taxis.add(t);
	}
	/**
	 * @REQUIRES:(this.candidate_taxis!=null)&&(t!=null);
	 * @MODIFIES:None;
	 * @EFFECTS:(this.candidate_taxis.contains(t)==true)==>(\result==true)&&
	 * 			(this.candidate_taxis.contains(t)==false)==>(\result==false);
	 */ 
	public boolean containTaxi(Taxi t)
	{
		if(candidate_taxis.contains(t))
		{
			return true;
		}else
		{
			return false;
		}
	}
	/**
	 * @REQUIRES:(this.printer!=null)&&(this.outputInfo!=null);
	 * @MODIFIES:None;
	 * @EFFECTS:output this.outputInfo to the file of printer;
	 */ 
	public void logReqInfo()
	{
		printer.print(this.outputInfo);
	}
	/**
	 * @REQUIRES:(this.candidate_taxis!=null);
	 * @MODIFIES:None;
	 * @EFFECTS:\exist Taxi taxi;this.candidate_taxis.contains(taxi)&&(isOptimalTaxi(taxi)==true)==>(\result==taxi);
	 * 			\all Taxi taxi;this.candidate_taxis.contains(taxi)&&(isOptimalTaxi(taxi)==false)==>(\result==null);
	 */ 
	public Taxi getOptimalTaxi()//从候选出租车队列中选择最优的出租车，没找到则返回null
	{
		ArrayList<Taxi> topTaxis=new ArrayList<Taxi>();
		ArrayList<Taxi> topTaxis2=new ArrayList<Taxi>();
		int firstTag=1;
		for(Taxi t:candidate_taxis)
		{
			if(t.getState()==TaxiState.IDLE)
			{
				if(firstTag==1)
				{
					topTaxis.add(t);
					firstTag=0;
				}else
				{
					if(t.getCredit()>topTaxis.get(0).getCredit())
					{
						topTaxis.clear();
						topTaxis.add(t);
					}else if(t.getCredit()==topTaxis.get(0).getCredit())
					{
						topTaxis.add(t);
					}
				}
			}
		}
		if(topTaxis.size()>1)//多于1个出租车，选最近的
		{
			firstTag=1;
			int mindis=-1;
			for(Taxi t:topTaxis)
			{
					if(firstTag==1)
					{
						topTaxis2.add(t);
						mindis=map.getminDistance(src, t.getLoc());
						firstTag=0;
					}else
					{
						if(map.getminDistance(src, t.getLoc())<mindis)
						{
							topTaxis2.clear();
							topTaxis2.add(t);
						}else if(map.getminDistance(src, t.getLoc())==mindis)
						{
							topTaxis2.add(t);
						}
					}
			}
			//多于1个出租车最近，则随机选一个，就选第一个
			return topTaxis2.get(0);
		}else if(topTaxis.size()==1)
		{
			return topTaxis.get(0);
		}else//没有符合的出租车
		{
			return null;
		}
	}
	
	/**
	 * @REQUIRES:(this.candidate_taxis!=null);
	 * @MODIFIES:taxis;
	 * @EFFECTS:(getOptimalTaxi()==null)==>(\result==false);
	 * 			(getOptimalTaxi()!=null)==>(\result==true)&&getOptimalTaxi().setServingReq(this);
	 */ 
	public boolean selectOneTaxi()//7500ms窗口期到了，从候选出租车中选择一辆
	{
		int num = candidate_taxis.size();
		Taxi taxi=null;
		if(num==0)//没有出租车响应请求
		{
			//System.out.println("tag 0");
			outputInfo+="#没有出租车接单\r\n";
			return false;
		}else
		{
			//记录候选出租车的信息
			outputInfo+="#所有抢单的出租车的信息:\r\n";
			for(Taxi t:candidate_taxis)
			{
				outputInfo+="车辆编号: "+t.getId()+"\t车辆位置: ("+ (int)t.getLoc().getX()+","+(int)t.getLoc().getY() + ")\t车辆状态: "
							+t.getState().toString()+"\t车辆信用: "+t.getCredit()+"\r\n";
			}
			taxi = getOptimalTaxi();//选择最优的出租车
			if(taxi==null)//没有出租车响应请求
			{
				outputInfo+="#没有出租车接单\r\n";
				return false;
			}else
			{
				//System.out.println("distribute!!!!!");
				outputInfo+="#被派单的车辆运行信息:\r\n车辆编号: "+taxi.getId()+"\t派单时的车辆位置坐标: ("
						+ (int)taxi.getLoc().getX()+","+(int)taxi.getLoc().getY() + ")\t派单时刻: "+(reqTime+7500)+"ms\r\n";
				taxi.setServingReq(this);//将该请求赋予出租车，并改变出租车的状态为PICK
				return true;
			}
		}
	}

}
