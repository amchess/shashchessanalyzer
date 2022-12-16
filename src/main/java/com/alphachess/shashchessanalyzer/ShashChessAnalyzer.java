package com.alphachess.shashchessanalyzer;

import static java.lang.String.format;
import static net.andreinc.neatchess.client.breaks.Break.breakOn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.andreinc.neatchess.client.model.Move;
import net.andreinc.neatchess.client.model.option.EngineOption;

/**
 * Hello world!
 *
 */
public class ShashChessAnalyzer {
	private static final int CAPABLANCA_THRESHOLD = 9;
	private static final int LOW_THRESHOLD = 20;
	private static final int LOW_MIDDLE_THRESHOLD = 41;
	private static final int MIDDLE_THRESHOLD = 59;
	private static final int HIGH_MIDDLE_THRESHOLD = 93;
	private static final int HIGH_THRESHOLD = 160;
	private static final String HIGH_PETROSIAN = "High Petrosian";
	private static final String HIGH_MIDDLE_PETROSIAN = "Middle High Petrosian";
	private static final String MIDDLE_PETROSIAN = "Middle Petrosian";
	private static final String MIDDLE_LOW_PETROSIAN = "Middle Low Petrosian";
	private static final String LOW_PETROSIAN = "Low Petrosian";
	private static final String CAOS_PETROSIAN_CAPABLANCA = "Caos Petrosian-Capablanca";
	private static final String CAPABLANCA = "Capablanca";
	private static final String CAOS_TAL_CAPABLANCA = "Caos Capablanca-Tal";
	private static final String LOW_TAL = "Low Tal";
	private static final String LOW_MIDDLE_TAL = "Low Middle Tal";
	private static final String MIDDLE_TAL = "Middle Tal";
	private static final String MIDDLE_HIGH_TAL = "Middle High Tal";
	private static final String HIGH_TAL = "High Tal";
	private static final String CAOS_TAL_CAPABLANCA_PETROSIAN = "Caos Tal-Capablanca-Petrosian";
	UCI uci = null;
	private Properties shashChessAnalyzerProperties;
	private int threadsNumber;
	private int hashSizeMB;
	private int cpuMhz;
	private String syzygyPath;
	private String syzygyProbeDepth;
	private long strongestAverageTimeSecondsForMove;
	private long timeoutSeconds;
	private long timeoutMS;
	private String fen;
	private int multiPV;
	private String fullDepthThreads;
	private String openingVariety;
	private String persistedLearning;
	private String readOnlyLearning;
	private String mcts;
	private String mCTSThreads;
	private String engineName;
	private String searchMoves;
	private String showEngineInfos;

	Logger logger = Logger.getLogger(ShashChessAnalyzer.class.getName());

	public ShashChessAnalyzer(String[] args) {
		shashChessAnalyzerProperties = getShashChessAnalyzerProperties(args);
		setInputParameters();
		setTimeoutMS(timeoutSeconds * 1000);
		uci = new UCI(timeoutMS);
	}

	private void setInputParameters() {
		setTimeoutSeconds(Long.parseLong(shashChessAnalyzerProperties.getProperty("timeoutSeconds")));
		setThreadsNumber(Integer.parseInt(shashChessAnalyzerProperties.getProperty("threadsNumber")));
		setCpuMhz(Integer.parseInt(shashChessAnalyzerProperties.getProperty("cpuMhz")));
		setHashSizeMB(Integer.parseInt(shashChessAnalyzerProperties.getProperty("hashSizeMB")));
		setSyzygyPath(shashChessAnalyzerProperties.getProperty("syzygyPath"));
		setSyzygyProbeDepth(shashChessAnalyzerProperties.getProperty("syzygyProbeDepth"));
		setStrongestAverageTimeSecondsForMove(getStrongestAverageTimeSeconds());
		setFen(shashChessAnalyzerProperties.getProperty("fen"));
		setMultiPV(Integer.parseInt(shashChessAnalyzerProperties.getProperty("multiPV")));
		setFullDepthThreads(shashChessAnalyzerProperties.getProperty("fullDepthThreads"));
		setOpeningVariety(shashChessAnalyzerProperties.getProperty("openingVariety"));
		setPersistedLearning(shashChessAnalyzerProperties.getProperty("persistedLearning"));
		setReadOnlyLearning(shashChessAnalyzerProperties.getProperty("readOnlyLearning"));
		setMcts(shashChessAnalyzerProperties.getProperty("mcts"));
		setMCTSThreads(shashChessAnalyzerProperties.getProperty("mCTSThreads"));
		setEngineName(shashChessAnalyzerProperties.getProperty("engineName"));
		setSearchMoves(shashChessAnalyzerProperties.getProperty("searchMoves"));
		setShowEngineInfos(shashChessAnalyzerProperties.getProperty("showEngineInfos"));
	}

	public long getStrongestAverageTimeSeconds() {
		return (long) (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getShashChessAnalyzerProperties(String[] args) {
		String shashChessAnalyzerProperties = args[0];
		Properties properties = new Properties();
		try {
			File file = new File(shashChessAnalyzerProperties);
			FileInputStream fileInput = new FileInputStream(file);
			properties.load(fileInput);
			fileInput.close();

		} catch (FileNotFoundException e) {
			logger.info(e.getMessage());
			System.exit(0);
		} catch (IOException e) {
			logger.info(e.getMessage());
			System.exit(0);
		}
		return properties;
	}

	public static void main(String[] args) {

		ShashChessAnalyzer shashChessAnalyzer = new ShashChessAnalyzer(args);
		try {
			shashChessAnalyzer.startShashChess();
			shashChessAnalyzer.setInitialUciOptions();
			String showEngineInfos = shashChessAnalyzer.getShowEngineInfos();
			if ((showEngineInfos != null) && (!showEngineInfos.isEmpty())
					&& (showEngineInfos.equalsIgnoreCase("yes"))) {
				shashChessAnalyzer.retrieveShashChessInfo();
			}
			shashChessAnalyzer.analyzePosition(1);
			shashChessAnalyzer.closeShashChess();
		} catch (Exception e) {
			shashChessAnalyzer.closeShashChess();
			System.out.println("End computation for timeout");
			System.exit(0);
		}
	}

	private void analyzePosition(int timeMultiplier) {
		long currentAverageTimeSecondsForMove = strongestAverageTimeSecondsForMove * 1000 * timeMultiplier * multiPV;
		System.out.println(String.join("", "Average time for all moves in seconds: ",
				Long.toString(currentAverageTimeSecondsForMove / 1000)));
		uci.uciNewGame();
		uci.positionFen(fen);
		String goCommand = (searchMoves != null && !searchMoves.isEmpty())
				? String.join("", "go movetime %d ", "searchmoves ", searchMoves)
				: "go movetime %d";
		UCIResponse<Analysis> response = uci.command(format(goCommand, (long) currentAverageTimeSecondsForMove),
				UCI.analysis::process, breakOn("bestmove"), uci.getDefaultTimeout());
		Analysis analysis = response.getResultOrThrow();
		/*
		 * Move bestMove = analysis.getBestMove(); System.out.println(String.join("",
		 * "Best move: ", bestMove.toString())); int score = ((Double)
		 * (bestMove.getStrength().getScore() * 100)).intValue();
		 * System.out.println(String.join("", "Score: ", Integer.toString(score),
		 * "cp")); String positionType = getPositionType(score);
		 * System.out.println(String.join("", "Position type: ", positionType));
		 */
		// Possible best moves
		Map<Integer, Move> moves = analysis.getAllMoves(); 
		Move bestMove = moves.get(1);
		System.out.println(String.join("", "Best move: ", bestMove.toString()));
		int score = ((Double) (bestMove.getStrength().getScore() * 100)).intValue();
		System.out.println(String.join("", "Score: ", Integer.toString(score), "cp"));
		String positionType = getPositionType(score);
		System.out.println(String.join("", "Position type: ", positionType));        
		moves.forEach((idx, move) -> {
			System.out.println(String.join("", "\t" + move));
		});
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		Date date = new Date();
		System.out.println(formatter.format(date));
		System.out.println(String.join("", "Restart the analysis based on Shashin theory - ", formatter.format(date)));
		setShashinUciOptions(positionType);
		analyzePosition(++timeMultiplier);
		closeShashChess();
	}

	private void setShashinUciOptions(String positionType) {
		switch (positionType) {
		case HIGH_PETROSIAN:
			uci.setOption(HIGH_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case HIGH_MIDDLE_PETROSIAN:
			uci.setOption(HIGH_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_PETROSIAN:
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_LOW_PETROSIAN:
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case LOW_PETROSIAN:
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case CAOS_PETROSIAN_CAPABLANCA:
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			break;
		case CAPABLANCA:
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			break;
		case CAOS_TAL_CAPABLANCA:
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case LOW_TAL:
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case LOW_MIDDLE_TAL:
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_TAL:
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_HIGH_TAL:
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(HIGH_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case HIGH_TAL:
			uci.setOption(HIGH_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case CAOS_TAL_CAPABLANCA_PETROSIAN:
			uci.setOption(HIGH_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(HIGH_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		default:
			break;
		}

	}

	private String getPositionType(int score) {
		if (score <= -HIGH_THRESHOLD) {
			return HIGH_PETROSIAN;
		}
		if ((score > -HIGH_THRESHOLD) && (score <= -HIGH_MIDDLE_THRESHOLD)) {
			return HIGH_MIDDLE_PETROSIAN;
		}
		if ((score > -HIGH_MIDDLE_THRESHOLD) && (score <= -MIDDLE_THRESHOLD)) {
			return MIDDLE_PETROSIAN;
		}
		if ((score > -MIDDLE_THRESHOLD) && (score <= -LOW_MIDDLE_THRESHOLD)) {
			return MIDDLE_LOW_PETROSIAN;
		}
		if ((score > -LOW_MIDDLE_THRESHOLD) && (score <= -LOW_THRESHOLD)) {
			return LOW_PETROSIAN;
		}
		if ((score > -LOW_THRESHOLD) && (score <= -CAPABLANCA_THRESHOLD)) {
			return CAOS_PETROSIAN_CAPABLANCA;
		}
		if ((score > -CAPABLANCA_THRESHOLD) && (score < CAPABLANCA_THRESHOLD)) {
			return CAPABLANCA;
		}
		if ((score >= CAPABLANCA_THRESHOLD) && (score < LOW_THRESHOLD)) {
			return CAOS_TAL_CAPABLANCA;
		}
		if ((score >= LOW_THRESHOLD) && (score < LOW_MIDDLE_THRESHOLD)) {
			return LOW_TAL;
		}
		if ((score >= LOW_MIDDLE_THRESHOLD) && (score < MIDDLE_THRESHOLD)) {
			return LOW_MIDDLE_TAL;
		}
		if ((score >= MIDDLE_THRESHOLD) && (score < HIGH_MIDDLE_THRESHOLD)) {
			return MIDDLE_TAL;
		}
		if ((score >= HIGH_MIDDLE_THRESHOLD) && (score < HIGH_THRESHOLD)) {
			return MIDDLE_HIGH_TAL;
		}
		if (score >= HIGH_THRESHOLD) {
			return HIGH_TAL;
		}
		return CAOS_TAL_CAPABLANCA_PETROSIAN;
	}

	private void setInitialUciOptions() {
		try {
			uci.setOption("Threads", Integer.toString(threadsNumber), timeoutMS).getResultOrThrow();
			uci.setOption("Hash", Integer.toString(hashSizeMB), timeoutMS).getResultOrThrow();
			uci.setOption("SyzygyPath", syzygyPath, timeoutMS).getResultOrThrow();
			uci.setOption("SyzygyProbeDepth", syzygyProbeDepth, timeoutMS).getResultOrThrow();
			uci.setOption("Full depth threads", fullDepthThreads, timeoutMS).getResultOrThrow();
			uci.setOption("Opening variety", openingVariety, timeoutMS).getResultOrThrow();
			uci.setOption("Persisted learning", persistedLearning, timeoutMS).getResultOrThrow();
			uci.setOption("Read only learning", readOnlyLearning, timeoutMS).getResultOrThrow();
			uci.setOption("MCTS", mcts, timeoutMS).getResultOrThrow();
			uci.setOption("MCTSThreads", mCTSThreads, timeoutMS).getResultOrThrow();
			uci.setOption("MultiPV", Integer.toString(multiPV));
		} catch (Exception e) {
			closeShashChess();
			System.out.println("Impossible to setup uci options");
			System.exit(0);
		}
	}

	@SuppressWarnings("rawtypes")
	private void retrieveShashChessInfo() {
		UCIResponse<EngineInfo> response = uci.getEngineInfo();
		if (response.success()) {

			// Engine name
			EngineInfo engineInfo = response.getResult();
			System.out.println(String.join("", "Engine name:" + engineInfo.getName()));

			// Supported engine options
			System.out.println("Supported engine options:");
			Map<String, EngineOption> engineOptions = engineInfo.getOptions();
			engineOptions.forEach((key, value) -> {
				System.out.println(String.join("", "\t", key));
				System.out.println(String.join("", "\t\t", value.toString()));
			});
		}
	}

	private void closeShashChess() {
		uci.close();
		System.out.println("Engine closed");
		System.exit(0);
	}

	private void startShashChess() {
		String engineNameWithExtension = String.join("", engineName,
				(System.getProperty("os.name").contains("Windows") ? ".exe" : ""));
		uci.start(engineNameWithExtension);
	}

	public UCI getUci() {
		return uci;
	}

	public void setUci(UCI uci) {
		this.uci = uci;
	}

	public int getHashSizeMB() {
		return hashSizeMB;
	}

	public void setHashSizeMB(int hashSizeMB) {
		this.hashSizeMB = hashSizeMB;
	}

	public int getCpuMhz() {
		return cpuMhz;
	}

	public void setCpuMhz(int cpuMhz) {
		this.cpuMhz = cpuMhz;
	}

	public String getSyzygyPath() {
		return syzygyPath;
	}

	public void setSyzygyPath(String syzygyPath) {
		this.syzygyPath = syzygyPath;
	}

	public String getSyzygyProbeDepth() {
		return syzygyProbeDepth;
	}

	public void setSyzygyProbeDepth(String syzygyProbeDepth) {
		this.syzygyProbeDepth = syzygyProbeDepth;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public Properties getShashChessAnalyzerProperties() {
		return shashChessAnalyzerProperties;
	}

	public long getTimeoutMS() {
		return timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.timeoutMS = timeoutMS;
	}

	public int getMultiPV() {
		return multiPV;
	}

	public void setMultiPV(int multiPV) {
		this.multiPV = multiPV;
	}

	public int getThreadsNumber() {
		return threadsNumber;
	}

	public void setThreadsNumber(int threadsNumber) {
		this.threadsNumber = threadsNumber;
	}

	public long getStrongestAverageTimeSecondsForMove() {
		return strongestAverageTimeSecondsForMove;
	}

	public void setStrongestAverageTimeSecondsForMove(long strongestAverageTimeSecondsForMove) {
		this.strongestAverageTimeSecondsForMove = strongestAverageTimeSecondsForMove;
	}

	public String getFen() {
		return fen;
	}

	public long getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public void setFen(String fen) {
		this.fen = fen;
	}

	public String getFullDepthThreads() {
		return fullDepthThreads;
	}

	public String getOpeningVariety() {
		return openingVariety;
	}

	public String getPersistedLearning() {
		return persistedLearning;
	}

	public String getReadOnlyLearning() {
		return readOnlyLearning;
	}

	public String getMcts() {
		return mcts;
	}

	public String getMCTSThreads() {
		return mCTSThreads;
	}

	public void setFullDepthThreads(String fullDepthThreads) {
		this.fullDepthThreads = fullDepthThreads;
	}

	public void setOpeningVariety(String openingVariety) {
		this.openingVariety = openingVariety;
	}

	public void setPersistedLearning(String persistedLearning) {
		this.persistedLearning = persistedLearning;
	}

	public void setReadOnlyLearning(String readOnlyLearning) {
		this.readOnlyLearning = readOnlyLearning;
	}

	public void setMcts(String mcts) {
		this.mcts = mcts;
	}

	public void setMCTSThreads(String mCTSThreads) {
		this.mCTSThreads = mCTSThreads;
	}

	public String getEngineName() {
		return engineName;
	}

	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	public String getSearchMoves() {
		return searchMoves;
	}

	public void setSearchMoves(String searchMoves) {
		this.searchMoves = searchMoves;
	}

	public String getShowEngineInfos() {
		return showEngineInfos;
	}

	public void setShowEngineInfos(String showEngineInfos) {
		this.showEngineInfos = showEngineInfos;
	}
}
