package taxi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestList {
		private BlockingQueue<Request> reqList;
		
		RequestList()
		{
			this.reqList=new LinkedBlockingQueue<Request>();
		}
		/**
		* @REQUIRES: this.reqList!=null;
		* @MODIFIES : None;
		* @EFFECTS : \result == this.reqList; 
		*/
		synchronized BlockingQueue<Request> getreqList()
		{
			return reqList;
		}
		/**
		* @REQUIRES: this.reqList!=null;
		* @MODIFIES : None;
		* @EFFECTS : \result == this.reqList.poll(); 
		*/
		synchronized Request poll() {
	        return reqList.poll();
	    }
		/**
		* @REQUIRES: this.reqList!=null;
		* @MODIFIES : None;
		* @EFFECTS : \result == this.reqList.peek(); 
		*/
	    synchronized Request peek() {
	        return reqList.peek();
	    }
	    /**
		* @REQUIRES: this.reqList!=null;
		* @MODIFIES : None;
		* @EFFECTS : (this.reqList.size()==0==>\result == true)&&(this.reqList.size()>0==>\result == false);	 
		*/
	    synchronized boolean isEmpty() {
	        return reqList.isEmpty();
	    }
	    /**
		* @REQUIRES: this.reqList!=null&&req!=null;
		* @MODIFIES : this.reqList;
		* @EFFECTS : this.reqList.offer(req); 
		*/
	    synchronized void offer(Request req) {
	    	reqList.offer(req);
	    }
	    /**
		* @REQUIRES: this.reqList!=null;
		* @MODIFIES : None;
		* @EFFECTS : \result==this.reqList.size(); 
		*/
	    synchronized int getSize()
	    {
	    	return reqList.size();
	    }
	    /**
		* @REQUIRES: this.reqList!=null&&req!=null;
		* @MODIFIES : None;
		* @EFFECTS : \exist Request r;this.reqList.contains(r)&&(r==req)==>(\result==true);
		*/
	    synchronized boolean contain(Request req)
	    {
	    	for(Request r: reqList)
	    	{
	    		if(r.getSrc().equals(req.getSrc())&&r.getDst().equals(req.getDst())&&r.getReqTime()==req.getReqTime())
	    		{
	    			return true;
	    		}
	    	}
	    	return false;
	    }

}
