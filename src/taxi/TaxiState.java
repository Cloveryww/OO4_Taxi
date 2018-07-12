package taxi;

public enum TaxiState {
	IDLE,STOP,PICK,WORK;
public String EnumToString() 
{ 
    String tmpStr = ""; 
    switch(this) 
    { 
        case IDLE: 
        	tmpStr = "IDLE"; 
        	break; 
        case STOP:
        	tmpStr = "STOP"; 
        	break;
        case PICK:
        	tmpStr = "PICK"; 
        	break;
        case WORK:
        	tmpStr = "WORK"; 
        	break;
        default:
        	break;
    } 
    return tmpStr; 
} 
}
