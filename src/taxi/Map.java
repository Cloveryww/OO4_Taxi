package taxi;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

class Map implements Runnable{
	public int[][] map=new int[TaxiSystem.MAPSIZE][TaxiSystem.MAPSIZE]; 
	public int[][] matrix=new int[TaxiSystem.MAPSIZE2][TaxiSystem.MAPSIZE2];//存储i点到j点的邻接矩阵，为了考虑车流量，有连接值为1，没有则为MAXVALUE
	public int[][] minDistances=new int[TaxiSystem.MAPSIZE2][TaxiSystem.MAPSIZE2];//存储i点到j点的最短距离
	public int[][][] flow = new int[TaxiSystem.MAPSIZE][TaxiSystem.MAPSIZE][2];//当前道路的流量，每500ms更新一次,[][][0]表示改点下面的道路，[][][1]表示该点右边的道路
    private int[][][] flow2 = new int[TaxiSystem.MAPSIZE][TaxiSystem.MAPSIZE][2];//统计最近500ms内的道路流量，每500ms将值赋给flow
    private static int MAXVALUE=10000000;
    private TaxiGUI gui;
	Map(TaxiGUI gui)
	{
		this.gui = gui;
		for(int i=0;i<TaxiSystem.MAPSIZE;i++)
		{
			for(int j=0;j<TaxiSystem.MAPSIZE;j++)
			{
				flow[i][j][0]=0;
				flow[i][j][1]=0;
				flow2[i][j][0]=0;
				flow2[i][j][1]=0;
			}
		}
		for(int i=0;i<TaxiSystem.MAPSIZE2;i++)
		{
			for(int j=0;j<TaxiSystem.MAPSIZE2;j++)
			{
				minDistances[i][j]=MAXVALUE;
			}
		}
	}
    
	/**
	 * @REQUIRES:(src.getX()>=0)&&(src.getX<80)&&(src.getY()>=0)&&(src.getY<80)
	 * 			&&(dst.getX()>=0)&&(dst.getX<80)&&(dst.getY()>=0)&&(dst.getY<80);
	 * @MODIFIES:None;
	 * @EFFECTS:(\result==minDistances[(int)(src.getX()*80+src.getY())][(int)(dst.getX()*80+dst.getY())];		
	 */ 
    public int getminDistance(Point src,Point dst)
	{
    	synchronized(this.minDistances) {//加锁
    		return minDistances[(int)(src.getX()*TaxiSystem.MAPSIZE+src.getY())][(int)(dst.getX()*TaxiSystem.MAPSIZE+dst.getY())];
    	}
	}
    /**
	 * @REQUIRES:(matrix!=null)&&(minDistances!=null);
	 * @MODIFIES:minDistances;
	 * @EFFECTS:\all int i,j;0<=i<80,0<=j<80;minDistances[i][j]==shortest distance between i and j;
	 */ 
	public void updateminDistances()//使用floyd算法更新最短距离矩阵，当开关道路和初始化时需要调用（复杂度有些高）
	{
		// 计算最短路径
		//minDistances=matrix.clone();
		for(int i=0;i<TaxiSystem.MAPSIZE2;i++)
		{
			for(int j=0;j<TaxiSystem.MAPSIZE2;j++)
			{
				minDistances[i][j]=matrix[i][j];
			}
		}
		synchronized(this.minDistances) {//加锁
			int tmp;
			for (int k = 0; k < TaxiSystem.MAPSIZE; k++)
		    {
		        for (int i = 0; i < TaxiSystem.MAPSIZE; i++)
		        {
		            for (int j = 0; j < TaxiSystem.MAPSIZE; j++)
		            {
		                // 如果经过下标为k顶点路径比原两点间路径更短，则更新minDistances[i][j]
		                tmp = (minDistances[i][k]==MAXVALUE || minDistances[k][j]==MAXVALUE) ? MAXVALUE : (minDistances[i][k] + minDistances[k][j]);
		                if (minDistances[i][j] > tmp)
		                {
		                	minDistances[i][j] = tmp;
		                }
		            }
		        }
		    }
		}
	}
	
	/**
	 * @REQUIRES:(flow!=null)&&(map!=null)&&(p!=null)&&(p.getX()>=0)&&(p.getX<80)&&(p.getY()>=0)&&(p.getY<80);
	 * @MODIFIES:None;
	 * @EFFECTS:\all Road road;\result.contains(road)==>
	 * 			(((map[(int)p.getX()-1][(int)p.getY()]==2||map[(int)p.getX()-1][(int)p.getY()]==3)==>(road.dir==0))&&
	 * 			(map[(int)p.getX()][(int)p.getY()-1]==1||map[(int)p.getX()][(int)p.getY()-1]==3)==>(road.dir==2)&&
	 * 			(map[(int)p.getX()][(int)p.getY()]==1)==>(road.dir==3)&&
	 * 			(map[(int)p.getX()][(int)p.getY()]==2)==>(road.dir==1)&&
	 * 			(map[(int)p.getX()][(int)p.getY()]==3)==>(road.dir==3)||(road.dir==1))&&(road.flow==this edge`s flow);
	 */ 
	public ArrayList<Road> getAdjustRoads(Point p)//获得某点关联的边
	{
		ArrayList<Road> re = new ArrayList<Road>();
		if(map[(int)p.getX()][(int)p.getY()]==1)
		{
			re.add(new Road(flow[(int)p.getX()][(int)p.getY()][1],3));
		}else if(map[(int)p.getX()][(int)p.getY()]==2)
		{
			re.add(new Road(flow[(int)p.getX()][(int)p.getY()][0],1));
		}else if(map[(int)p.getX()][(int)p.getY()]==3)
		{
			re.add(new Road(flow[(int)p.getX()][(int)p.getY()][1],3));
			re.add(new Road(flow[(int)p.getX()][(int)p.getY()][0],1));
		}
		if(p.getX()>0)//有上面的点
		{
			if(map[(int)p.getX()-1][(int)p.getY()]==2||map[(int)p.getX()-1][(int)p.getY()]==3)
			{
				re.add(new Road(flow[(int)p.getX()-1][(int)p.getY()][0],0));
			}
		}
		
		if(p.getY()>0)//有左边的点
		{
			if(map[(int)p.getX()][(int)p.getY()-1]==1||map[(int)p.getX()][(int)p.getY()-1]==3)
			{
				re.add(new Road(flow[(int)p.getX()][(int)p.getY()-1][1],2));
			}
		}
		return re;
	}
	/**
	 * @REQUIRES:(map!=null)&&(index<6400);
	 * @MODIFIES:None;
	 * @EFFECTS:\all int i;\result.contains(i)==>
	 * 					((((i-index)==1)&&(i/80==index/80)==>(map[index/80][index%80]==3)||(map[index/80][index%80]==1))&&
	 * 					 (((i-index)==-1)&&(i/80==index/80)==>(map[i/80][i%80]==3)||(map[i/80][i%80]==1))&&	
	 * 					 (((i-index)==80)==>(map[index/80][index%80]==2)||(map[index/80][index%80]==3))&&
	 * 					 (((i-index)==-80)==>(map[i/80][i%80]==2)||(map[i/80][i%80]==3)));
	 */ 
	public Vector<Integer> getAdjustNodesIndex(int index)//获得某点关联的点的一维索引,输入也是某点的一维索引
	{
		Vector<Integer> re = new Vector<Integer>();
		if(map[(int)(index/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE)]==1)
		{
			re.add(index+1);
		}else if(map[(int)(index/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE)]==2)
		{
			re.add(index+TaxiSystem.MAPSIZE);
		}else if(map[(int)(index/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE)]==3)
		{
			re.add(index+1);
			re.add(index+TaxiSystem.MAPSIZE);
		}
		if((int)(index/TaxiSystem.MAPSIZE)>0)//有上面的点
		{
			if(map[(int)((index-TaxiSystem.MAPSIZE)/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE)]==2||map[(int)((index-TaxiSystem.MAPSIZE)/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE)]==3)
			{
				re.add(index-TaxiSystem.MAPSIZE);
			}
		}
		
		if((int)(index%TaxiSystem.MAPSIZE)>0)//有左边的点
		{
			if(map[(int)(index/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE-1)]==1||map[(int)(index/TaxiSystem.MAPSIZE)][(int)(index%TaxiSystem.MAPSIZE-1)]==3)
			{
				re.add(index-1);
			}
		}
		return re;
	}
	
	/**
	 * @REQUIRES:(flow2!=null)&&(src.getX()>=0)&&(src.getX<80)&&(src.getY()>=0)&&(src.getY<80)
	 * 				&&(dst.getX()>=0)&&(dst.getX<80)&&(dst.getY()>=0)&&(dst.getY<80);
	 * @MODIFIES:flow2;
	 * @EFFECTS: (((src.getX()-dst.getX())==-1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	(flow2[(int)src.getX()][(int)src.getY()][1]==\old(flow2[(int)src.getX()][(int)src.getY()][1])+1))&&
	 * 			 (((src.getX()-dst.getX())==1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	(flow2[(int)dst.getX()][(int)dst.getY()][1]==\old(flow2[(int)dst.getX()][(int)dst.getY()][1])+1))&&
	 *			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==-1)==>
	 * 			 	(flow2[(int)src.getX()][(int)src.getY()][0]==\old(flow2[(int)src.getX()][(int)src.getY()][0])+1))&&
	 * 			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==1)==>
	 * 			 	(flow2[(int)dst.getX()][(int)dst.getY()][0]==\old(flow2[(int)dst.getX()][(int)dst.getY()][0])+1));
	 */ 
	public void addflow(Point src,Point dst)
	{
		double dx = src.getX()-dst.getX();
        double dy = src.getY()-dst.getY();
        if(dx==-1&&dy==0)
        {
        	flow2[(int)src.getX()][(int)src.getY()][1]++;
        }else if(dx==1&&dy==0)
        {
        	flow2[(int)dst.getX()][(int)dst.getY()][1]++;
        }else if(dx==0&&dy==-1)
        {
        	flow2[(int)src.getX()][(int)src.getY()][0]++;
        }else if(dx==0&&dy==1)
        {
        	flow2[(int)dst.getX()][(int)dst.getY()][0]++;
        }
	}
	/**
	 * @REQUIRES:(flow!=null)&&(src.getX()>=0)&&(src.getX<80)&&(src.getY()>=0)&&(src.getY<80)
	 * 				&&(dst.getX()>=0)&&(dst.getX<80)&&(dst.getY()>=0)&&(dst.getY<80)&&(value>=0);
	 * @MODIFIES:flow;
	 * @EFFECTS: (((src.getX()-dst.getX())==-1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	(flow[(int)src.getX()][(int)src.getY()][1]==value)&&
	 * 			 (((src.getX()-dst.getX())==1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	(flow[(int)dst.getX()][(int)dst.getY()][1]==value)&&
	 *			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==-1)==>
	 * 			 	(flow[(int)src.getX()][(int)src.getY()][0]==value)&&
	 * 			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==1)==>
	 * 			 	(flow[(int)dst.getX()][(int)dst.getY()][0]==value);
	 */ 
	public void setflow(Point src,Point dst,int value)
	{
		double dx = src.getX()-dst.getX();
        double dy = src.getY()-dst.getY();
        if(dx==-1&&dy==0)
        {
        	flow[(int)src.getX()][(int)src.getY()][1]=value;
        }else if(dx==1&&dy==0)
        {
        	flow[(int)dst.getX()][(int)dst.getY()][1]=value;
        }else if(dx==0&&dy==-1)
        {
        	flow[(int)src.getX()][(int)src.getY()][0]=value;
        }else if(dx==0&&dy==1)
        {
        	flow[(int)dst.getX()][(int)dst.getY()][0]=value;
        }
	}
	
	/**
	 * @REQUIRES:(flow!=null)&&(src.getX()>=0)&&(src.getX<80)&&(src.getY()>=0)&&(src.getY<80)
	 * 				&&(dst.getX()>=0)&&(dst.getX<80)&&(dst.getY()>=0)&&(dst.getY<80);
	 * @MODIFIES:None;
	 * @EFFECTS: (((src.getX()-dst.getX())==-1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	\result==flow[(int)src.getX()][(int)src.getY()][1])&&
	 * 			 (((src.getX()-dst.getX())==1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	\result==flow[(int)dst.getX()][(int)dst.getY()][1])&&
	 *			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==-1)==>
	 * 			 	\result==flow[(int)src.getX()][(int)src.getY()][0])&&
	 * 			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==1)==>
	 * 			 	\result==flow[(int)dst.getX()][(int)dst.getY()][0]);
	 */ 
	public int getOneflow(Point src,Point dst)
	{
		double dx = src.getX()-dst.getX();
        double dy = src.getY()-dst.getY();
        if(dx==-1&&dy==0)
        {
        	return flow[(int)src.getX()][(int)src.getY()][1];
        }else if(dx==1&&dy==0)
        {
        	return flow[(int)dst.getX()][(int)dst.getY()][1];
        }else if(dx==0&&dy==-1)
        {
        	return flow[(int)src.getX()][(int)src.getY()][0];
        }else if(dx==0&&dy==1)
        {
        	return flow[(int)dst.getX()][(int)dst.getY()][0];
        }
        //System.out.println("getOneflow Error!");
        return -1;
	}
	/**
	 * @REQUIRES:(flow!=null)&&(src_index>=0)&&(src_index<6400)&&(dst_index>=0)&&(dst_index<6400);
	 * @MODIFIES:None;
	 * @EFFECTS: (((src.getX()-dst.getX())==-1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	\result==flow[(int)src.getX()][(int)src.getY()][1])&&
	 * 			 (((src.getX()-dst.getX())==1)&&((src.getY()-dst.getY())==0)==>
	 * 			 	\result==flow[(int)dst.getX()][(int)dst.getY()][1])&&
	 *			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==-1)==>
	 * 			 	\result==flow[(int)src.getX()][(int)src.getY()][0])&&
	 * 			 (((src.getX()-dst.getX())==0)&&((src.getY()-dst.getY())==1)==>
	 * 			 	\result==flow[(int)dst.getX()][(int)dst.getY()][0]);
	 */ 
	public int getOneflow(int src_index,int dst_index)//输入为一维索引
	{
		Point src=new Point((int)(src_index/TaxiSystem.MAPSIZE),(int)(src_index%TaxiSystem.MAPSIZE));
		Point dst=new Point((int)(dst_index/TaxiSystem.MAPSIZE),(int)(dst_index%TaxiSystem.MAPSIZE));
		double dx = src.getX()-dst.getX();
        double dy = src.getY()-dst.getY();
        if(dx==-1&&dy==0)
        {
        	return flow[(int)src.getX()][(int)src.getY()][1];
        }else if(dx==1&&dy==0)
        {
        	return flow[(int)dst.getX()][(int)dst.getY()][1];
        }else if(dx==0&&dy==-1)
        {
        	return flow[(int)src.getX()][(int)src.getY()][0];
        }else if(dx==0&&dy==1)
        {
        	return flow[(int)dst.getX()][(int)dst.getY()][0];
        }
        //System.out.println("getOneflow Error!");
        return -1;
	}
	/**
	 * @REQUIRES:FILE.exist(path),System.in;
	 * @MODIFIES:map;
	 * @EFFECTS: read map from file;
	 */ 
	public void readmap(String path){//读入地图信息
		Scanner scan=null;
		File file=new File(path);
		if(file.exists()==false){
			System.out.println("地图文件不存在,程序退出");
			System.exit(1);
			return;
		}
		try {
			scan = new Scanner(new File(path));
		} catch (FileNotFoundException e) {
			System.out.println("地图文件不存在,程序退出");
			System.exit(1);
			return;
		}
		for(int i=0;i<TaxiSystem.MAPSIZE;i++){
			String[] strArray = null;
			try{
				strArray=scan.nextLine().split("");
			}catch(Exception e){
				System.out.println("地图文件信息有误，程序退出");
				System.exit(1);
			}
			for(int j=0;j<TaxiSystem.MAPSIZE;j++){
				try{
					this.map[i][j]=Integer.parseInt(strArray[j]);
					if(this.map[i][j]>4||this.map[i][j]<0)
					{
						System.out.println("地图文件信息有误，程序退出");
						System.exit(1);
					}
				}catch(Exception e){
					System.out.println("地图文件信息有误，程序退出");
					System.exit(1);
				}
			}
		}
		scan.close();
		
		init_matrix();
		
	}
	
	/**
	 * @REQUIRES:matrix!=null&&map!=null;
	 * @MODIFIES:matrix;
	 * @EFFECTS: init matrix according to map;
	 */ 
	public void init_matrix()
	{
		//初始化邻接矩阵
		for(int i=0;i<TaxiSystem.MAPSIZE;i++)
		{
			for(int j=0;j<TaxiSystem.MAPSIZE;j++)
			{
				matrix[i*TaxiSystem.MAPSIZE+j][i*TaxiSystem.MAPSIZE+j]=0;
				if(map[i][j]==1)
				{
					matrix[i*TaxiSystem.MAPSIZE+j][i*TaxiSystem.MAPSIZE+j+1]=1;
				}else if(map[i][j]==2)
				{
					matrix[i*TaxiSystem.MAPSIZE+j][i*TaxiSystem.MAPSIZE+j+TaxiSystem.MAPSIZE]=1;
				}else if(map[i][j]==3)
				{
					matrix[i*TaxiSystem.MAPSIZE+j][i*TaxiSystem.MAPSIZE+j+1]=1;
					matrix[i*TaxiSystem.MAPSIZE+j][i*TaxiSystem.MAPSIZE+j+TaxiSystem.MAPSIZE]=1;
				}
			}
		}
	}
	/**
	 * @REQUIRES:matrix!=null&&map!=null&&minDistances!=null;
	 * @MODIFIES:map,matrix,minDistances,gui;
	 * @EFFECTS: (haveEdge((src_x,src_y),(dst_x,dst_y))==false)==>(haveEdge((src_x,src_y),(dst_x,dst_y))==true);
	 * 			&&updateminDistances();
	 */ 
	public void openOneRoad(int src_x,int src_y,int dst_x,int dst_y)
	{
		int dx=src_x-dst_x;
		int dy=src_y-dst_y;
		if((dx==1)&&(dy==0))
		{
			if(map[dst_x][dst_y]==1)//之前是关着的，则打开
			{
				map[dst_x][dst_y]=3;
			}else if(map[dst_x][dst_y]==0)//之前是关着的，则打开
			{
				map[dst_x][dst_y]=2;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else if((dx==-1)&&(dy==0))
		{
			if(map[src_x][src_y]==1)//之前是关着的，则打开
			{
				map[src_x][src_y]=3;
			}else if(map[src_x][src_y]==0)//之前是关着的，则打开
			{
				map[src_x][src_y]=2;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else if((dx==0)&&(dy==1))
		{
			if(map[dst_x][dst_y]==2)//之前是关着的，则打开
			{
				map[dst_x][dst_y]=3;
			}else if(map[dst_x][dst_y]==0)//之前是关着的，则打开
			{
				map[dst_x][dst_y]=1;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else if((dx==0)&&(dy==-1))
		{
			if(map[src_x][src_y]==2)//之前是关着的，则打开
			{
				map[src_x][src_y]=3;
			}else if(map[src_x][src_y]==0)//之前是关着的，则打开
			{
				map[src_x][src_y]=1;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else
		{
			System.out.println("Order invalid");
			return;
		}
		matrix[src_x*TaxiSystem.MAPSIZE+src_y][dst_x*TaxiSystem.MAPSIZE+dst_y]=1;
		matrix[dst_x*TaxiSystem.MAPSIZE+dst_y][src_x*TaxiSystem.MAPSIZE+src_y]=1;
		updateminDistances();
		gui.SetRoadStatus(new Point(src_x,src_y), new Point(dst_x,dst_y), 1);
	}
	/**
	 * @REQUIRES:matrix!=null&&map!=null&&minDistances!=null;
	 * @MODIFIES:map,matrix,minDistances,gui;
	 * @EFFECTS: (haveEdge((src_x,src_y),(dst_x,dst_y))==true)==>(haveEdge((src_x,src_y),(dst_x,dst_y))==false);
	 * 			&&updateminDistances();
	 */ 
	public void closeOneRoad(int src_x,int src_y,int dst_x,int dst_y)
	{
		int dx=src_x-dst_x;
		int dy=src_y-dst_y;
		if((dx==1)&&(dy==0))
		{
			if(map[dst_x][dst_y]==2)//之前是开着的，则关闭
			{
				map[dst_x][dst_y]=0;
			}else if(map[dst_x][dst_y]==3)//之前是开着的，则关闭
			{
				map[dst_x][dst_y]=1;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else if((dx==-1)&&(dy==0))
		{
			if(map[src_x][src_y]==2)//之前是开着的，则关闭
			{
				map[src_x][src_y]=0;
			}else if(map[src_x][src_y]==3)//之前是开着的，则关闭
			{
				map[src_x][src_y]=1;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else if((dx==0)&&(dy==1))
		{
			if(map[dst_x][dst_y]==1)//之前是开着的，则关闭
			{
				map[dst_x][dst_y]=0;
			}else if(map[dst_x][dst_y]==3)//之前是开着的，则关闭
			{
				map[dst_x][dst_y]=2;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else if((dx==0)&&(dy==-1))
		{
			if(map[src_x][src_y]==1)//之前是开着的，则关闭
			{
				map[src_x][src_y]=0;
			}else if(map[src_x][src_y]==3)//之前是开着的，则关闭
			{
				map[src_x][src_y]=2;
			}
			else
			{
				System.out.println("Order invalid,this road has been opened");
				return;
			}
		}else
		{
			System.out.println("Order invalid");
			return;
		}
		matrix[src_x*TaxiSystem.MAPSIZE+src_y][dst_x*TaxiSystem.MAPSIZE+dst_y]=MAXVALUE;
		matrix[dst_x*TaxiSystem.MAPSIZE+dst_y][src_x*TaxiSystem.MAPSIZE+src_y]=MAXVALUE;
		updateminDistances();
		gui.SetRoadStatus(new Point(src_x,src_y), new Point(dst_x,dst_y), 0);
	}

	@Override
	public void run() {
		long time_wucha=0;
        long start_time = gv.getTime();
        while (true) {
            time_wucha = (gv.getTime()-start_time)%500;
            try{
            	if(time_wucha<50)
            	{
                Thread.sleep(500-time_wucha);
            	}else
            	{
            		Thread.sleep(500);
            	}
            } catch (Exception e) {}
            synchronized (this) {
            	for(int i=0;i<TaxiSystem.MAPSIZE;i++)
        		{
        			for(int j=0;j<TaxiSystem.MAPSIZE;j++)
        			{
        				flow[i][j][0]=flow2[i][j][0];
        				flow[i][j][1]=flow2[i][j][1];
        				flow2[i][j][0]=0;
        				flow2[i][j][1]=0;
        			}
        		}
            }
        }
		
	}
}
class Road
{
	public int flow;//车流量
	public int dir;//方向,0:向上，1:向下,2:向左,3:向右
	public Road(int flow, int dir) {
		super();
		this.flow = flow;
		this.dir = dir;
	}
	
}

