package vort;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import robocode.AdvancedRobot;
import robocode.BulletHitEvent;
import robocode.ScannedRobotEvent;

class ScannedData
{
	public double X;
	public double Y;
	int tick;
	ScannedRobotEvent rawData;
	
    public ScannedData(AdvancedRobot myRobot, ScannedRobotEvent scanned, int tick)
    {
        rawData = scanned;
        double absAngle = myRobot.getHeadingRadians() + scanned.getBearingRadians();
        X = myRobot.getX() + Math.sin(absAngle) * scanned.getDistance();
        Y = myRobot.getY() + Math.cos(absAngle) * scanned.getDistance();
        System.out.println("Scanned: " + X + ", " + Y);
        this.tick = tick;
    }	
    
    public boolean IsExpired(int tick)
    {
        return (tick - this.tick) > 10;
    }
}

public class Chaser extends AdvancedRobot
{
	Map<String, ScannedData> scanned;
	int tick;
	
    public void onBulletHit(BulletHitEvent evnt)
    {
        if (evnt.getEnergy() <= 0.0)
            if (scanned.containsKey(evnt.getName()))
                scanned.remove(evnt.getName());
    }
    
    private ScannedData GetNearestBot()
    {
    	double minDist = 0.0;
    	ScannedData minDistBot = null;
        for (ScannedData bot : scanned.values())
        {
            double deltaX = bot.X - getX();
            double deltaY = bot.Y - getY();
            double vlen = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (minDistBot == null)
            {
            	minDist = vlen;
            	minDistBot = bot;
            }
            else
            {
            	if (vlen < minDist)
            	{
            		minDist = vlen;
            		minDistBot = bot;
            	}
            }
        }
        return minDistBot;
    }
    
	public void run()
	{
		tick = 0;
		scanned = new HashMap<String, ScannedData>();
		
        for (; ; )
        {
        	Map<String, ScannedData> scannedNew = 
        			new HashMap<String, ScannedData>();
        	for (Entry<String, ScannedData> kv : scanned.entrySet())
        	{
        		if (!kv.getValue().IsExpired(tick))
        			scannedNew.put(kv.getKey(), kv.getValue());
        	}
    		scanned = scannedNew;

            if (scanned.size() == 0)
            {
                setTurnRadarRight(360);
            }
            else
            {
                ScannedData sd = GetNearestBot();
                double deltaX = sd.X - getX();
                double deltaY = sd.Y - getY();
                double vlen = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double vxn = deltaX / vlen;
                double vyn = deltaY / vlen;
                double angle = Math.acos(vyn) * 180 / Math.PI;
                if (vxn < 0)
                    angle = 360 - angle;

                double turnAngle = FixTurnAngle(angle - getHeading());
                setTurnRight(turnAngle);
                double radarTurnAngle = FixTurnAngle(angle - getRadarHeading());
                setTurnRadarRight(radarTurnAngle);

                setAhead(vlen - 60);

                if (Math.abs(turnAngle) < 2.0)
                    setFire(vlen < 80 ? 3.0 : 1.0);

                scan();
            }
            execute();
            tick++;
        }
	}
	
    private double FixTurnAngle(double angle)
    {
        double result = angle;
        while (result > 180)
            result -= 360;
        while (result < -180)
            result += 360;
        return result;
    }
    
	public void onScannedRobot(ScannedRobotEvent evnt)
	{
		scanned.put(evnt.getName(), new ScannedData(this, evnt, tick));
	}
}
