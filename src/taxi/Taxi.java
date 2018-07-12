package taxi;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Taxi implements Runnable{
    private Point loc;//出租车当前位置
    private TaxiState state;//出租车当前状态
    private int id;//出租车编号
    private int credit;//出租车信用,抢一次单+1，完成一单+3
    private Request servingReq;
    private TaxiGUI taxiGUI;//GUI
    private Map map;
    private long start_time;//出租车最开始的时间
    private long cur_time;//当前时间
    
    Taxi(int id,Map map,TaxiGUI taxiGUI)
    {
    	this.id = id;
    	this.map = map;
    	this.taxiGUI = taxiGUI;
    	this.state = TaxiState.STOP;
    	int num = (int)(Math.random()*TaxiSystem.MAPSIZE2);
    	this.loc=new Point((int)(num/TaxiSystem.MAPSIZE),(int)(num%TaxiSystem.MAPSIZE));
    	this.credit = 0;
    	long t=System.currentTimeMillis();
		this.start_time=t-t%100;
    	this.cur_time = 0;
    	//System.out.println("Taxi "+id+"start_time="+start_time);
    }
    

    synchronized public Request getServingReq() {
		return servingReq;
	}
    /**
	 * @REQUIRES:(this!=null)&&(servingReq!=null);
	 * @MODIFIES:this.state,this.servingReq;
	 * @EFFECTS:(this.state == TaxiState.PICK)&&(this.servingReq == servingReq);	
	 */ 
    synchronized public void setServingReq(Request servingReq) {
    	this.state = TaxiState.PICK;
		this.servingReq = servingReq;
	}

	public Point getLoc() {
		return loc;
	}

	public void setLoc(Point loc) {
		this.loc = loc;
	}

	synchronized public TaxiState getState() {
		return state;
	}

	synchronized public void setState(TaxiState state) {
		this.state = state;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	synchronized public int getCredit() {
		return credit;
	}

	synchronized public void setCredit(int credit) {
		this.credit = credit;
	}
	/**
	 * @REQUIRES:(map!=null)&&(target!=null)&&(target.getX()>=0)&&(target.getX()<80)&&(target.getY()>=0)&&(target.getY()<80);
	 * @MODIFIES:None;
	 * @EFFECTS:(\result == index)&&(nexts[(this.loc.getX()*80+this.loc.getY())]==index)&&(nexts[] is shortest path array);
	 */ 
	private int getNextPoint_index(Point target)//使用SPFA算法(类似BFS)获得最短路径并且车流量最小的下一个位置,
	{
		int start_index = (int)(target.getX()*TaxiSystem.MAPSIZE+target.getY());//从目的地开始找最短路径
        int loc_index = (int)(loc.getX()*TaxiSystem.MAPSIZE+loc.getY());//当前位置的索引（一维）
        boolean[] inQueue = new boolean[TaxiSystem.MAPSIZE2];//inQueue记录节点是否在优先队列中
        
        int[] dists = new int[TaxiSystem.MAPSIZE2];//距离数组,表示到target的距离
        int[] flows = new int[TaxiSystem.MAPSIZE2];//流量数组,表示点i到target的车流量之和
        int[] nexts = new int[TaxiSystem.MAPSIZE2];//next[i]表示点i的下一个点
        Queue<Integer> queue = new LinkedList<>();//优先队列
        //初始化
        for (int i = 0; i < TaxiSystem.MAPSIZE2; i++) {
            dists[i] = Integer.MAX_VALUE;
            flows[i] = Integer.MAX_VALUE;
        }
        
        dists[start_index] = 0;
        flows[start_index] = 0;
        queue.offer(start_index);//将target加入队列，开始算法
        inQueue[start_index] = true;
        int cur;//当前节点的索引
        int distance, sumOfFlow;
        while (!queue.isEmpty()) //直到收敛
        {
        	cur = queue.poll();//取出队首
            Vector<Integer> adjNodes = map.getAdjustNodesIndex(cur);
            for (int to : adjNodes) {
                distance = 1 + dists[cur];//距离加1
                sumOfFlow = flows[cur] + map.getOneflow(cur, to);
                if (distance<dists[to]||(distance==dists[to]&&sumOfFlow<flows[to])) 
                {
                    nexts[to] = cur;//跟新to节点的后继
                    dists[to] = distance;
                    flows[to] = sumOfFlow;
                    if (!inQueue[to])//不在队列里的化加入队列
                    {
                        queue.offer(to);
                        inQueue[to] = true;
                    }
                }
            }
            inQueue[cur] = false;
        }
        //收敛了
        if(Math.abs((nexts[loc_index]-loc_index))!=TaxiSystem.MAPSIZE&&Math.abs((nexts[loc_index]-loc_index))!=1)
        {
        	System.out.println("SPFA error!");
        }
        return nexts[loc_index];//获得下一步位置的一维索引	
	}
	
	/**
	 * @REQUIRES:(map!=null)&&(this.state==IDLE);
	 * @MODIFIES:this.loc,map,flow2;
	 * @EFFECTS:(\this.loc==adjnode(\old(this.loc))==true)&&(map.addflow(\old(this.loc), this.loc));
	 */ 
	private void IDLE_move()//等待服务状态下进行移动
	{
		ArrayList<Road> roads = map.getAdjustRoads(loc);
		ArrayList<Road> roads_rand = new ArrayList<Road>();
		Road sel=null;
		int first=1;
		for(Road r:roads)
		{
			if(first==1)
			{
				first=0;
				roads_rand.add(r);
			}else
			{
				if(r.flow<roads_rand.get(0).flow)
				{
					roads_rand.clear();
					roads_rand.add(r);
				}else if(r.flow==roads_rand.get(0).flow)
				{
					roads_rand.add(r);
				}
			}
		}
		if(roads_rand.size()>1)//流量最小的分支有多个，则随机选一个
		{
			int no = (int)(roads_rand.size()*Math.random());
			sel = roads_rand.get(no);
		}else
		{
			sel = roads_rand.get(0);
		}
		Point pre = new Point(loc);
		switch(sel.dir)
		{
		case 0:
			loc.setLocation(loc.getX()-1,loc.getY());
			break;
		case 1:
			loc.setLocation(loc.getX()+1,loc.getY());
			break;
		case 2:
			loc.setLocation(loc.getX(),loc.getY()-1);
			break;
		case 3:
			loc.setLocation(loc.getX(),loc.getY()+1);
			break;
		default:
			break;	
		}
		map.addflow(pre, loc);//添加车流量
		return;
	}
	/**
	 * @REQUIRES:(map!=null)&&((isPick==true)==>(this.state==PICK))&&((isPick==false)==>(this.state==WORK));
	 * @MODIFIES:this.loc,map,flow2;
	 * @EFFECTS:(\this.loc==adjnode(\old(this.loc))==true)&&(map.addflow(\old(this.loc), this.loc));
	 */ 
	private void SERVE_move(boolean isPick)//服务状态中进行移动(包括PICK 和 WORK)
	{
		Point pre = new Point(loc);
		if(isPick)//PICK状态
		{
			int next_index = getNextPoint_index(servingReq.getSrc());
			loc.setLocation((int)(next_index/TaxiSystem.MAPSIZE), (int)(next_index%TaxiSystem.MAPSIZE));
			
		}else//WORK状态
		{
			int next_index = getNextPoint_index(servingReq.getDst());
			loc.setLocation((int)(next_index/TaxiSystem.MAPSIZE), (int)(next_index%TaxiSystem.MAPSIZE));
		}
		map.addflow(pre, loc);//添加车流量
		return;
	}
	@Override
	public void run() {
		long time_wucha;
		int stop_clock=40;
		while(true)//无限循环，状态转移
		{
			if (state == TaxiState.STOP) {//停止服务中1s
				taxiGUI.SetTaxiStatus(id,loc,0);//更新GUI
                time_wucha = (gv.getTime()-start_time)%100;
                
                try {
                	if (time_wucha<30)
                	{
                		Thread.sleep(1000-time_wucha);//减小误差
                	}else
                	{
                		Thread.sleep(1000);
                	}
                } catch (Exception e) {}
                cur_time+=1000;//改变当前出租车的时间
                this.setState(TaxiState.IDLE);
                stop_clock = 40;//没20s IDLE 就 stop 1s，初始化倒计时
			}else if(state == TaxiState.IDLE)//等待服务状态
			{
				taxiGUI.SetTaxiStatus(id,loc,2);//更新GUI
				time_wucha = (gv.getTime() - start_time)%500;
				 try {
	                	if (time_wucha<50)
	                	{
	                		Thread.sleep(500-time_wucha);//减小误差
	                	}else
	                	{
	                		Thread.sleep(500);
	                	}
	                } catch (Exception e) {}
	            cur_time+=500;//改变当前出租车的时间
	            IDLE_move();//“随机”移动
				stop_clock--;
				if(state==TaxiState.IDLE&&stop_clock==0)
				{
					this.setState(TaxiState.STOP);
				}
            } else if (state == TaxiState.PICK) {//正在去接顾客
            	taxiGUI.SetTaxiStatus(id,loc,1);//更新GUI
               if (loc.equals(servingReq.getSrc())) //到达起始地点，接上了顾客
               {
                    this.servingReq.outputInfo+="达到乘客位置的时刻: "+cur_time+"ms\t乘客位置坐标: ("+(int)this.servingReq.getSrc().getX()+","+(int)this.servingReq.getSrc().getY()+")\r\n";
                    time_wucha = (gv.getTime()-start_time)%100;
                   
                    try {//停止1s模拟上车过程
                    	if (time_wucha<50)
                    	{
                    		Thread.sleep(1000-time_wucha);
                    	}else{
                    		Thread.sleep(1000);
                    	}
                    } catch (Exception e) {}
                    cur_time+=1000;
                    this.setState(TaxiState.WORK);//改变状态
                } else {
                	time_wucha = (gv.getTime()-start_time)%100;
                	 //System.out.println(time_wucha);
                     try {
                     	if (time_wucha<50)
                     	{
                     		Thread.sleep(500-time_wucha);
                     	}else{
                     		Thread.sleep(500);
                     	}
                     } catch (Exception e) {}
                     cur_time+=500;
                     SERVE_move(true);//朝着起始地运行一步
                }
            } else if (state == TaxiState.WORK) {//正在去往用户目的地
            	 taxiGUI.SetTaxiStatus(id, loc, 1);
            	if (loc.equals(servingReq.getDst())) //到达目的地，结束了订单
                {
            		 this.servingReq.outputInfo+="到达目的地时刻: "+cur_time+"ms\t到达目的地坐标: ("+(int)this.servingReq.getDst().getX()+","+(int)this.servingReq.getDst().getY()+")\r\n";
                     this.servingReq.logReqInfo();
            		 this.setState(TaxiState.STOP);//改变状态
                 } else {
                	 this.servingReq.outputInfo+="中间点 时刻: "+cur_time+"ms\t坐标: ("+(int)loc.getX()+","+(int)loc.getY()+")\r\n";
                	 time_wucha = (gv.getTime()-start_time)%100;
                 	// System.out.println(time_wucha);
                      try {
                      	if (time_wucha<50)
                      	{
                      		Thread.sleep(500-time_wucha);
                      	}else{
                      		Thread.sleep(500);
                      	}
                      } catch (Exception e) {}
                      SERVE_move(false);//朝着目的地运行一步
                      cur_time+=500;
                 }
            } 
            
		}//end while
	}
}
