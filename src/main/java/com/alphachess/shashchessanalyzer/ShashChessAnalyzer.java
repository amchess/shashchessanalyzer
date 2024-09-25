package com.alphachess.shashchessanalyzer;

import static java.lang.String.format;
import static net.andreinc.neatchess.client.breaks.Break.breakOn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.alphachess.shashchessanalyzer.WinProbabilityByMaterial.RangeDescription;

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
	private static Logger logger = Logger.getLogger(ShashChessAnalyzer.class.getName());

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
		return (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getShashChessAnalyzerProperties(String[] args) {
		String shashChessAnalyzerPropertiesFromArgs = args[0];
		Properties properties = new Properties();
		File file = new File(shashChessAnalyzerPropertiesFromArgs);

		try (FileInputStream fileInput = new FileInputStream(file)) {
			properties.load(fileInput);
		} catch (IOException e) {
			logger.info(e.getMessage());
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
			logger.info("End computation for timeout");
			System.exit(0);
		}
	}

	private void analyzePosition(int timeMultiplier) {
		long currentAverageTimeSecondsForMove = strongestAverageTimeSecondsForMove * 1000 * timeMultiplier * multiPV;
		String analyzeMsg = String.join("", "Average time for all moves in seconds: ",
				Long.toString(currentAverageTimeSecondsForMove / 1000));
		logger.info(analyzeMsg);
		uci.uciNewGame();
		fen = fen.trim();
		uci.positionFen(fen);
		UCIResponse<Analysis> response = null;
		if (searchMoves == null || searchMoves.isEmpty()) {
			response = uci.analysis(currentAverageTimeSecondsForMove);
		} else {
			String goCommand = (!searchMoves.isEmpty())
					? String.join("", "go movetime %d ", "searchmoves ", searchMoves)
					: "go movetime %d";
			response = uci.command(format(goCommand, currentAverageTimeSecondsForMove), UCI.analysis::process,
					breakOn("bestmove"), uci.getDefaultTimeout());
		}
		Analysis analysis = response.getResultOrThrow();
		Map<Integer, Move> moves = analysis.getAllMoves();
		Move bestMove = moves.get(1);
		String besMoveMsg = String.join("", "Best move: ", bestMove.toString());
		logger.info(besMoveMsg);
		int score = ((Double) (bestMove.getStrength().getScore() * 100)).intValue();
		String scoreMsg = String.join("", "Score: ", Integer.toString(score), "cp");
		logger.info(scoreMsg);
		String positionType = getPositionType(score, fen);
		String positionTypeMsg = String.join("", "Position type: ", positionType);
		logger.info(positionTypeMsg);
		String winProbability = Integer.toString(WinProbabilityByMaterial.getWinProbabilityByScore(score, fen));
		String winProbabilityMsg = String.join("", "Win Probability: ", winProbability);
		logger.info(winProbabilityMsg);

		moves.forEach((idx, move) -> logger.info(String.join("", "\t" + move)));
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
		Date date = new Date();
		String dateMsg = formatter.format(date);
		logger.info(dateMsg);
		String restartMsg = String.join("", "Restart the analysis based on Shashin theory - ", dateMsg);
		logger.info(restartMsg);
		setShashinUciOptions(positionType);
		analyzePosition(++timeMultiplier);
		closeShashChess();
	}

	private void setShashinUciOptions(String positionType) {
		RangeDescription[] rangeDescriptions = RangeDescription.values();
		RangeDescription rangeDescription = null;
		for (RangeDescription currentRangeDescription : rangeDescriptions) {
			if (currentRangeDescription.getDescription().equals(positionType)) {
				rangeDescription = currentRangeDescription;
				break;
			}
		}
		if (rangeDescription != null) {
			switch (rangeDescription) {
			case HIGH_PETROSIAN:
			case MIDDLE_PETROSIAN:
			case LOW_PETROSIAN:
			case CAPABLANCA:
			case LOW_TAL:
			case MIDDLE_TAL:
			case HIGH_TAL:
				uci.setOption(rangeDescription.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case HIGH_MIDDLE_PETROSIAN:
				uci.setOption(RangeDescription.HIGH_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.MIDDLE_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case MIDDLE_LOW_PETROSIAN:
				uci.setOption(RangeDescription.MIDDLE_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.LOW_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case CAOS_PETROSIAN_CAPABLANCA:
				uci.setOption(RangeDescription.LOW_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.CAPABLANCA.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case CAOS_TAL_CAPABLANCA:
				uci.setOption(RangeDescription.CAPABLANCA.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.LOW_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case LOW_MIDDLE_TAL:
				uci.setOption(RangeDescription.LOW_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case MIDDLE_HIGH_TAL:
				uci.setOption(RangeDescription.MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.HIGH_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case CAOS_TAL_CAPABLANCA_PETROSIAN:
				setAllPersonalities();
				break;
			default:
				break;
			}
		}
	}

	private void setAllPersonalities() {
		uci.setOption(RangeDescription.HIGH_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.MIDDLE_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.LOW_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.CAPABLANCA.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.LOW_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.LOW_MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.HIGH_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
	}

	private String getPositionType(int score, String fen) {
		int winProbability = WinProbabilityByMaterial.getWinProbabilityByScore(score, fen);
		int range = WinProbabilityByMaterial.getRange(winProbability);
		return WinProbabilityByMaterial.getRangeDescription(range);
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
			logger.info("Impossible to setup uci options");
			System.exit(0);
		}
	}

	@SuppressWarnings("rawtypes")
	private void retrieveShashChessInfo() {
		UCIResponse<EngineInfo> response = uci.getEngineInfo();
		if (response.success()) {

			// Engine name
			EngineInfo engineInfo = response.getResult();
			String engineNameMsg = String.join("", "Engine name:" + engineInfo.getName());
			logger.info(engineNameMsg);

			// Supported engine options
			logger.info("Supported engine options:");
			Map<String, EngineOption> engineOptions = engineInfo.getOptions();
			engineOptions.forEach((key, value) -> {
				logger.info(String.join("", "\t", key));
				logger.info(String.join("", "\t\t", value.toString()));
			});
		}
	}

	private void closeShashChess() {
		uci.close();
		logger.info("Engine closed");
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
