package com.alphachess.shashchessanalyzer;

import static java.util.function.Function.identity;
import static net.andreinc.neatchess.client.breaks.Break.breakOn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	private static final int QUISCENT_THRESHOLD = 15;
	private static final int LOW_THRESHOLD = 35;
	private static final int MEDIUM_THRESHOLD = 70;
	private static final int HIGH_THRESHOLD = 140;
	private static final String CAOS_TAL_CAPABLANCA_PETROSIAN = "Caos Tal-Capablanca-Petrosian";
	private static final String HIGH_TAL = "High Tal";
	private static final String MEDIUM_TAL = "Medium Tal";
	private static final String LOW_TAL = "Low Tal";
	private static final String CAOS_TAL_CAPABLANCA = "Caos Tal-Capablanca";
	private static final String CAPABLANCA = "Capablanca";
	private static final String CAOS_CAPABLANCA_PETROSIAN = "Caos Capablanca-Petrosian";
	private static final String LOW_PETROSIAN = "Low Petrosian";
	private static final String MEDIUM_PETROSIAN = "Medium Petrosian";
	private static final String HIGH_PETROSIAN = "High Petrosian";
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
			// shashChessAnalyzer.retrieveShashChessInfo();
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
		System.out.println("Average time per move in seconds: " + currentAverageTimeSecondsForMove / 1000);
		uci.uciNewGame();
		uci.positionFen(fen);
		UCIResponse<Analysis> response = uci.analysis((long) currentAverageTimeSecondsForMove);
		Analysis analysis;

		analysis = response.getResultOrThrow();
		Move bestMove = analysis.getBestMove();
		System.out.println("Best move: " + bestMove);
		int score = ((Double) (bestMove.getStrength().getScore() * 100)).intValue();
		System.out.println("Score: " + score + "cp");
		String positionType = getPositionType(score);
		System.out.println("Position type: " + positionType);
		// Possible best moves
		Map<Integer, Move> moves = analysis.getAllMoves();
		moves.forEach((idx, move) -> {
			System.out.println("\t" + move);
		});
		System.out.println("Restart the analysis based on Shashin theory");
		setShashinUciOptions(positionType);
		analyzePosition(++timeMultiplier);
		closeShashChess();
	}

	private void setShashinUciOptions(String positionType) {
		switch (positionType) {
		case HIGH_PETROSIAN:
		case MEDIUM_PETROSIAN:
		case LOW_PETROSIAN:
			uci.setOption("Petrosian", "true", timeoutMS).getResultOrThrow();
		case CAOS_CAPABLANCA_PETROSIAN:
			uci.setOption("Petrosian", "true", timeoutMS).getResultOrThrow();
			uci.setOption("Capablanca", "true", timeoutMS).getResultOrThrow();
		case CAPABLANCA:
			uci.setOption("Capablanca", "true", timeoutMS).getResultOrThrow();
		case CAOS_TAL_CAPABLANCA:
			uci.setOption("Capablanca", "true", timeoutMS).getResultOrThrow();
			uci.setOption("Tal", "true", timeoutMS).getResultOrThrow();
		case HIGH_TAL:
		case MEDIUM_TAL:
		case LOW_TAL:
			uci.setOption("Tal", "true", timeoutMS).getResultOrThrow();
		}

	}

	private String getPositionType(int score) {
		if (score < -HIGH_THRESHOLD) {
			return HIGH_PETROSIAN;
		}
		if ((score >= -HIGH_THRESHOLD) && (score < -MEDIUM_THRESHOLD)) {
			return MEDIUM_PETROSIAN;
		}
		if ((score >= -MEDIUM_THRESHOLD) && (score < -LOW_THRESHOLD)) {
			return LOW_PETROSIAN;
		}
		if ((score >= -LOW_THRESHOLD) && (score < -QUISCENT_THRESHOLD)) {
			return CAOS_CAPABLANCA_PETROSIAN;
		}
		if ((score > -QUISCENT_THRESHOLD) && (score < QUISCENT_THRESHOLD)) {
			return CAPABLANCA;
		}
		if ((score >= QUISCENT_THRESHOLD) && (score <= LOW_THRESHOLD)) {
			return CAOS_TAL_CAPABLANCA;
		}
		if ((score > LOW_THRESHOLD) && (score <= MEDIUM_THRESHOLD)) {
			return LOW_TAL;
		}
		if ((score > MEDIUM_THRESHOLD) && (score <= HIGH_THRESHOLD)) {
			return MEDIUM_TAL;
		}
		if (score > HIGH_THRESHOLD) {
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
			System.out.println("Engine name:" + engineInfo.getName());

			// Supported engine options
			System.out.println("Supported engine options:");
			Map<String, EngineOption> engineOptions = engineInfo.getOptions();
			engineOptions.forEach((key, value) -> {
				System.out.println("\t" + key);
				System.out.println("\t\t" + value);
			});
		}
	}

	private void closeShashChess() {
		uci.close();
		System.out.println("Engine closed");
		System.exit(0);
	}

	private void startShashChess() {
		String engineNameWithExtension = engineName + (System.getProperty("os.name").contains("Windows") ? ".exe" : "");
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
}
