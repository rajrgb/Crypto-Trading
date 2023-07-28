package com.crypto.trade;

public class ChandelierExit {
private int period;
private int multiplier;
private double lowPrices[];
private double highPrices[];
private double closingPrices[];
private double prevAtr;
public ChandelierExit() {
	period=22;
	multiplier=3;
	prevAtr=-1;
}

public ChandelierExit(int period, int multiplier, double[] lowPrices, double[] highPrices, double[] closingPrices) {
	super();
	this.period = period;
	this.multiplier = multiplier;
	this.lowPrices = lowPrices;
	this.highPrices = highPrices;
	this.closingPrices = closingPrices;
}



public int getPeriod() {
	return period;
}

public void setPeriod(int period) {
	this.period = period;
}

public int getMultiplier() {
	return multiplier;
}

public void setMultiplier(int multiplier) {
	this.multiplier = multiplier;
}

public double[] getLowPrices() {
	return lowPrices;
}

public void setLowPrices(double[] lowPrices) {
	this.lowPrices = lowPrices;
}

public double[] getHighPrices() {
	return highPrices;
}

public void setHighPrices(double[] highPrices) {
	this.highPrices = highPrices;
}

public double[] getClosingPrices() {
	return closingPrices;
}

public void setClosingPrices(double[] closingPrices) {
	this.closingPrices = closingPrices;
}
public void setPrevAtr() {
	double tr[]=new double[period+1];
	for(int i=0;i<=period;i++)
	{
		tr[i]=Math.max(Math.abs(highPrices[i]-lowPrices[i]), 
				       Math.max( Math.abs(highPrices[i]-closingPrices[i+1]), Math.abs(lowPrices[i]-closingPrices[i+1])));
	}
	
	
	double sum=0;
	for(int i=1;i<=period;i++) sum+=tr[i];
	prevAtr=sum/period;
}

public void setPrevAtr(double atr) {
	prevAtr=atr;
}
public double getPrevAtr() {
	if(prevAtr==-1) setPrevAtr();
	return prevAtr;
}
public double calAtr() {
	
	
	double currTr=Math.max(Math.abs(highPrices[0]-lowPrices[0]), 
		       Math.max( Math.abs(highPrices[0]-closingPrices[1]), Math.abs(lowPrices[0]-closingPrices[1])));
	double atr=(getPrevAtr()*(period-1)+currTr)/period;
	
	setPrevAtr(atr);
	
	return atr;
}

public double calChandelierExitLong( ) {
	double max=highPrices[1];
	for(int i=2;i<=period+1;i++) {
		if(highPrices[i]>max) max=highPrices[i];
	}
	return max-calAtr()*multiplier;
}

public double calChandelierExitShort() {
	double min=lowPrices[1];
	for(int i=2;i<=period+1;i++) {
		if(lowPrices[i]<min) min=lowPrices[i];
	}
	return min+calAtr()*multiplier;
}

}