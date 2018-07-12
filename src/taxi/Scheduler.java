package taxi;

import java.util.ArrayList;

public class Scheduler implements Runnable{
	private Taxi[] taxis;
	private RequestList requestList;
	private Map map;
	private TaxiGUI gui;
	private long start_time;
	
	
	public Scheduler(Taxi[] taxis, RequestList requestList, Map map, TaxiGUI gui) {
		super();
		this.taxis = taxis;
		this.requestList = requestList;
		this.map = map;
		this.gui = gui;
		long t=System.currentTimeMillis();
		this.start_time=t-t%100;
		//System.out.println("Scheduler start_time="+start_time);
	}
	 /**
		 * @REQUIRES:(requestList!=null)&&(taxis!=null);
		 * @MODIFIES:taxis,requestList;
		 * @EFFECTS:\all Request req;requestList.contains(req);\all Taxi taxi;taxis.contains(taxi);((((taxi.getState()==TaxiState.IDLE)&&(taxi.getLoc().getX()>=(req.getSrc().getX()-2))
						&&(taxi.getLoc().getX()<=(req.getSrc().getX()+2))&&(taxi.getLoc().getY()>=(req.getSrc().getY()-2))
						&&(taxi.getLoc().getY()<=(req.getSrc().getY()+2))&&(!req.containTaxi(taxi)))==true)==>
						req.add_candidate_taxi(taxi)&&taxi.setCredit(taxi.getCredit()+1));	
		 */ 
	void callTaxi()//替请求寻找能够接单的出租车，找到后加入到请求的候选出租车集合中，直到7500ms窗口之间到选择最终出租车
	{
		for(Request req:requestList.getreqList())
		{
			for(int i=0;i<TaxiSystem.TAXINUM;i++)
			{
				Taxi taxi = taxis[i];
				if((taxi.getState()==TaxiState.IDLE)&&(taxi.getLoc().getX()>=(req.getSrc().getX()-2))
						&&(taxi.getLoc().getX()<=(req.getSrc().getX()+2))&&(taxi.getLoc().getY()>=(req.getSrc().getY()-2))
						&&(taxi.getLoc().getY()<=(req.getSrc().getY()+2))&&(!req.containTaxi(taxi)))
				{
					req.add_candidate_taxi(taxi);//将改出租车加入改请求的抢单队列中
					//System.out.println("enter candidate!");
					taxi.setCredit(taxi.getCredit()+1);//成功参与抢单一次，则信用立即+1
				}
			}
		}
	}
	@Override
	public void run() {
		
		while(true)
		{
			callTaxi();
			if(!requestList.isEmpty())//请求队列非空
			{
				//System.out.println("ReqList is not empty");
				long curTime = System.currentTimeMillis()-start_time;
				//System.out.println("Req time = "+requestList.peek().getReqTime()+"curTime= "+curTime);
				if((requestList.peek().getReqTime()+7500)<=curTime)//7500ms的窗口期到了
				{
					//System.out.println("7500ms has come");
					Request curReq = requestList.poll();//弹出第一个最早的请求
	                if (curReq.selectOneTaxi())
	                {
	                	//System.out.println(curReq.toString()+"successfully get a taxi");
	                }else {
	                	curReq.logReqInfo();
	                	System.out.println(curReq.toString()+"no taxi respons");
	                }
				}
			}
		}	
	}
	
	//以下是提供给测试者的测试接口
		public String queryOneTaxi(int No)//按照出租车查询状态信息，状态信息包括查询时刻、出租车当前坐标、当前所处状态。
		{
			String out="";
			long curTime = System.currentTimeMillis()-start_time;
			int loc_x,loc_y;
			loc_x=(int)taxis[No].getLoc().getX();
			loc_y=(int)taxis[No].getLoc().getY();
			TaxiState ts = taxis[No].getState();
			out = "出租车编号: "+No+"\t查询时刻: "+curTime+"ms\t出租车当前坐标: ("+loc_x+","+loc_y+")\t出租车当前状态: "+ts.toString();
			return out;
		}
		public ArrayList<Integer> queryTaxisNowithState(TaxiState ts)//返回所有在查询时刻处于相应状态的所有出租车编号
		{
			ArrayList<Integer> re=new ArrayList<Integer>();
			for(int i=0;i<TaxiSystem.MAPSIZE;i++)
			{
				if(taxis[i].getState().equals(ts))
				{
					re.add(i);
				}
			}
			return re;
		}
	
	
	

}
