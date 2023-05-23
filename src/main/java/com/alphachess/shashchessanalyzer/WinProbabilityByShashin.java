package com.alphachess.shashchessanalyzer;

public class WinProbabilityByShashin {
	public static final int MAX_DEPTH = 240;
	public static final int NORMALIZE_TO_PAWN_VALUE = 343;
	
	public static enum RangeDescription 
	{

		HIGH_PETROSIAN("High Petrosian"),
		HIGH_MIDDLE_PETROSIAN("Middle High Petrosian"),
		MIDDLE_PETROSIAN("Middle Petrosian"),
		MIDDLE_LOW_PETROSIAN("Middle Low Petrosian"),
		LOW_PETROSIAN("Low Petrosian"),
		CAOS_PETROSIAN_CAPABLANCA("Caos Petrosian-Capablanca"),
		CAPABLANCA("Capablanca"),
		CAOS_TAL_CAPABLANCA("Caos Capablanca-Tal"),
		LOW_TAL("Low Tal"),
		LOW_MIDDLE_TAL("Low Middle Tal"),
		MIDDLE_TAL("Middle Tal"),
		MIDDLE_HIGH_TAL("Middle High Tal"),
		HIGH_TAL("High Tal"),
		CAOS_TAL_CAPABLANCA_PETROSIAN("Caos Tal-Capablanca-Petrosian");
		
	    private String description;
	 
	    RangeDescription(String description) {
	        this.description = description;
	    }
	 
	    public String getDescription() {
	        return description;
	    }
	}
	public static String getAbbreviateRangeDescription(String rangeDescription) {
	    switch (rangeDescription) {
	        case MoveInfo.CAOS_TAL_CAPABLANCA_PETROSIAN:
	        	return "CTCP";
	        case MoveInfo.HIGH_PETROSIAN:
	        	return "HP";
	        case MoveInfo.HIGH_MIDDLE_PETROSIAN:
	        	return "MHP";
	        case MoveInfo.MIDDLE_PETROSIAN:
	        	return "MP";	
	        case MoveInfo.MIDDLE_LOW_PETROSIAN:
	        	return "MLP";
	        case MoveInfo.LOW_PETROSIAN:
	        	return "LP";
	        case MoveInfo.CAOS_PETROSIAN_CAPABLANCA:
	        	return "CCP";
	        case MoveInfo.CAPABLANCA:
	        	return "C";
	        case MoveInfo.CAOS_TAL_CAPABLANCA:
	        	return "CCT";
	        case MoveInfo.LOW_TAL:
	        	return "LT";
	        case MoveInfo.LOW_MIDDLE_TAL:
	        	return RangeDescription.LOW_MIDDLE_TAL.getDescription();
	        case MoveInfo.MIDDLE_TAL:
	        	return "MT";
	        case MoveInfo.MIDDLE_HIGH_TAL:
	        	return "MHT";
	        case MoveInfo.HIGH_TAL:
	        	return "HT";
	        default: return null;
	    }
	}	
	public String getRangeDescription(int rangeValue) {
	    Range[] values = Range.values();
	    Range range=null;
	    for(Range currentRange:values) {
	    	if(currentRange.getValue()==rangeValue) {
	    		range=currentRange;
	    		break;
	    	}
	    }
	    switch (range) {
	        case SHASHIN_POSITION_TAL_CAPABLANCA_PETROSIAN:
	        	return RangeDescription.CAOS_TAL_CAPABLANCA_PETROSIAN.getDescription();
	        case SHASHIN_POSITION_HIGH_PETROSIAN:
	        	return RangeDescription.HIGH_PETROSIAN.getDescription();
	        case SHASHIN_POSITION_MIDDLE_HIGH_PETROSIAN:
	        	return RangeDescription.HIGH_MIDDLE_PETROSIAN.getDescription();
	        case SHASHIN_POSITION_MIDDLE_PETROSIAN:
	        	return RangeDescription.MIDDLE_PETROSIAN.getDescription();	
	        case SHASHIN_POSITION_MIDDLE_LOW_PETROSIAN:
	        	return RangeDescription.MIDDLE_LOW_PETROSIAN.getDescription();
	        case SHASHIN_POSITION_LOW_PETROSIAN:
	        	return RangeDescription.LOW_PETROSIAN.getDescription();
	        case SHASHIN_POSITION_CAPABLANCA_PETROSIAN:
	        	return RangeDescription.CAOS_PETROSIAN_CAPABLANCA.getDescription();
	        case SHASHIN_POSITION_CAPABLANCA:
	        	return RangeDescription.CAPABLANCA.getDescription();
	        case SHASHIN_POSITION_CAPABLANCA_TAL:
	        	return RangeDescription.CAOS_TAL_CAPABLANCA.getDescription();
	        case SHASHIN_POSITION_LOW_TAL:
	        	return RangeDescription.LOW_TAL.getDescription();
	        case SHASHIN_POSITION_MIDDLE_LOW_TAL:
	        	return RangeDescription.LOW_MIDDLE_TAL.getDescription();
	        case SHASHIN_POSITION_MIDDLE_TAL:
	        	return RangeDescription.MIDDLE_TAL.getDescription();
	        case SHASHIN_POSITION_MIDDLE_HIGH_TAL:
	        	return RangeDescription.MIDDLE_HIGH_TAL.getDescription();
	        case SHASHIN_POSITION_HIGH_TAL:
	        	return RangeDescription.HIGH_TAL.getDescription();
	        default: return null;
	    }
	}
	public enum Range {
		  SHASHIN_POSITION_TAL_CAPABLANCA_PETROSIAN(7),
          SHASHIN_POSITION_HIGH_PETROSIAN(-6),
		  SHASHIN_POSITION_MIDDLE_HIGH_PETROSIAN(-5),
		  SHASHIN_POSITION_MIDDLE_PETROSIAN(-4),
		  SHASHIN_POSITION_MIDDLE_LOW_PETROSIAN(-3),
		  SHASHIN_POSITION_LOW_PETROSIAN(-2),
		  SHASHIN_POSITION_CAPABLANCA_PETROSIAN(-1),
		  SHASHIN_POSITION_CAPABLANCA(0),
		  SHASHIN_POSITION_CAPABLANCA_TAL(1),
		  SHASHIN_POSITION_LOW_TAL(2),
		  SHASHIN_POSITION_MIDDLE_LOW_TAL(3),
		  SHASHIN_POSITION_MIDDLE_TAL(4),
		  SHASHIN_POSITION_MIDDLE_HIGH_TAL(5),
		  SHASHIN_POSITION_HIGH_TAL(6);

		private int value;

		private Range(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}
	public enum ShashinThreeshold
	{
	  SHASHIN_LOW_TAL_THRESHOLD(76),
	  SHASHIN_MIDDLE_LOW_TAL_THRESHOLD(81),
	  SHASHIN_MIDDLE_TAL_THRESHOLD(88),
	  SHASHIN_MIDDLE_HIGH_TAL_THRESHOLD(91),
	  SHASHIN_HIGH_TAL_THRESHOLD(96),
	  SHASHIN_CAPABLANCA_THRESHOLD(51),
	  SHASHIN_LOW_PETROSIAN_THRESHOLD(24),
	  SHASHIN_MIDDLE_LOW_PETROSIAN_THRESHOLD(19),
	  SHASHIN_MIDDLE_PETROSIAN_THRESHOLD(12),
	  SHASHIN_MIDDLE_HIGH_PETROSIAN_THRESHOLD(9),
	  SHASHIN_HIGH_PETROSIAN_THRESHOLD(4);
		private int value;

		private ShashinThreeshold(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	};	
	public int getWinProbability(long score, int ply) {
		long value=(long)(score*NORMALIZE_TO_PAWN_VALUE/100);
		double winrateToMove=(0.5 + 1000 / (1 + Math.exp((((((1.07390458 * (Math.min(240, ply) / 64.0) + -6.94334517) * (Math.min(240, ply) / 64.0) + 31.95090161) * 
	    		(Math.min(240, ply) / 64.0)) + 317.75424048) - (GenericUtil.clamp((double)(value), -4000.0, 4000.0))) / ((((-2.82843814 * (Math.min(240, ply) / 64.0) + 16.64518180) * 
	    				(Math.min(240, ply) / 64.0) + -19.74439200) * (Math.min(240, ply) / 64.0)) + 68.39499088))));
		double winrateOpponent=(int)(0.5 + 1000 / (1 + Math.exp((((((1.07390458 * (Math.min(240, ply) / 64.0) + -6.94334517) * (Math.min(240, ply) / 64.0) + 31.95090161) * 
	    		(Math.min(240, ply) / 64.0)) + 317.75424048) - (GenericUtil.clamp((double)(-value), -4000.0, 4000.0))) / ((((-2.82843814 * (Math.min(240, ply) / 64.0) + 16.64518180) * 
	    				(Math.min(240, ply) / 64.0) -19.74439200) * (Math.min(240, ply) / 64.0)) + 68.39499088))));
		double  winrateDraw=1000-winrateToMove-winrateOpponent; 
		double winProbability=Math.round(winrateToMove+winrateDraw/2.0d)/10.0d;
		return (int)Math.round(winProbability);
	}
	public int getComplexityGap(int currentPlayedMoveWinProbability, int previousPlayedMoveWinProbability) {
		int currentPlayedMoveWinProbabilityRange=getRange(currentPlayedMoveWinProbability);
		int currentPreviousMoveWinProbabilityRange=getRange(previousPlayedMoveWinProbability);
		return Math.abs(currentPlayedMoveWinProbabilityRange-currentPreviousMoveWinProbabilityRange);
	}

	public int getRange(int winProbability) {
	    if (winProbability <= ShashinThreeshold.SHASHIN_HIGH_PETROSIAN_THRESHOLD.getValue())
	    {
	        return Range.SHASHIN_POSITION_HIGH_PETROSIAN.getValue();
	    }
	    if ((winProbability > ShashinThreeshold.SHASHIN_HIGH_PETROSIAN_THRESHOLD.getValue()) && (winProbability <= ShashinThreeshold.SHASHIN_MIDDLE_HIGH_PETROSIAN_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_MIDDLE_HIGH_PETROSIAN.getValue();
	    }
	    if ((winProbability > ShashinThreeshold.SHASHIN_MIDDLE_HIGH_PETROSIAN_THRESHOLD.getValue()) && (winProbability <= ShashinThreeshold.SHASHIN_MIDDLE_PETROSIAN_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_MIDDLE_PETROSIAN.getValue();
	    }
	    if ((winProbability > ShashinThreeshold.SHASHIN_MIDDLE_PETROSIAN_THRESHOLD.getValue()) && (winProbability <= ShashinThreeshold.SHASHIN_MIDDLE_LOW_PETROSIAN_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_MIDDLE_LOW_PETROSIAN.getValue();
	    }
	    if ((winProbability > ShashinThreeshold.SHASHIN_MIDDLE_LOW_PETROSIAN_THRESHOLD.getValue()) && (winProbability <= ShashinThreeshold.SHASHIN_LOW_PETROSIAN_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_LOW_PETROSIAN.getValue();
	    }
	    if ((winProbability > ShashinThreeshold.SHASHIN_LOW_PETROSIAN_THRESHOLD.getValue()) && (winProbability <= 100-ShashinThreeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_CAPABLANCA_PETROSIAN.getValue();
	    }
	    if ((winProbability > (100-ShashinThreeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue())) && (winProbability < ShashinThreeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_CAPABLANCA.getValue();
	    }
	    if ((winProbability < ShashinThreeshold.SHASHIN_LOW_TAL_THRESHOLD.getValue()) && (winProbability >= ShashinThreeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_CAPABLANCA_TAL.getValue();
	    }
	    if ((winProbability < ShashinThreeshold.SHASHIN_MIDDLE_LOW_TAL_THRESHOLD.getValue()) && (winProbability >= ShashinThreeshold.SHASHIN_LOW_TAL_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_LOW_TAL.getValue();
	    }
	    if ((winProbability < ShashinThreeshold.SHASHIN_MIDDLE_TAL_THRESHOLD.getValue()) && (winProbability >= ShashinThreeshold.SHASHIN_MIDDLE_LOW_TAL_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_MIDDLE_LOW_TAL.getValue();
	    }
	    if ((winProbability < ShashinThreeshold.SHASHIN_MIDDLE_HIGH_TAL_THRESHOLD.getValue()) && (winProbability >= ShashinThreeshold.SHASHIN_MIDDLE_TAL_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_MIDDLE_TAL.getValue();
	    }
	    if ((winProbability < ShashinThreeshold.SHASHIN_HIGH_TAL_THRESHOLD.getValue()) && (winProbability >= ShashinThreeshold.SHASHIN_MIDDLE_HIGH_TAL_THRESHOLD.getValue()))
	    {
	        return Range.SHASHIN_POSITION_MIDDLE_HIGH_TAL.getValue();
	    }
	    if (winProbability >=ShashinThreeshold.SHASHIN_HIGH_TAL_THRESHOLD.getValue())
	    {
	        return Range.SHASHIN_POSITION_HIGH_TAL.getValue();
	    }
	    return Range.SHASHIN_POSITION_TAL_CAPABLANCA_PETROSIAN.getValue();
	}
}
