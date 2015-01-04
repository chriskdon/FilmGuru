package com.monikle.neuro;

/**
 * Author:    Chris Kellendonk
 * Student #: 4810800
 * Date:      2015-01-04.
 */
public final class TrainerConfiguration {
	private int maxEpochs;
	private double acceptableError;
	private double validationAmount;
	private TrainingData trainingData;
	private boolean shuffleTrainingData;

	private TrainerConfiguration(TrainingData trainingData) {
		this.trainingData = trainingData;
	}

	public static TrainerConfiguration create(TrainingData trainingData) {
		if(trainingData == null) {
			throw new IllegalArgumentException("`trainingData` can't be null.");
		}

		return new TrainerConfiguration(trainingData);
	}

	public TrainerConfiguration setMaxEpochs(int count) {
		this.maxEpochs = count;
		return this;
	}

	public int getMaxEpochs() {
		return maxEpochs;
	}

	public double getAcceptableError() {
		return acceptableError;
	}

	public double getValidationAmount() {
		return validationAmount;
	}

	public TrainingData getTrainingData() {
		return trainingData;
	}

	public boolean getShuffleTrainingData() {
		return shuffleTrainingData;
	}

	public TrainerConfiguration setShuffleTrainingData(boolean shuffle) {
		this.shuffleTrainingData = shuffle;
		return this;
	}

	public TrainerConfiguration setAcceptableError(double error) {
		this.acceptableError = error;
		return this;
	}

	public TrainerConfiguration setValidationAmount(double amount) {
		if (amount < 0 || amount > 1) {
			throw new IllegalArgumentException("`amount` must be in range [0, 1].");
		}

		this.validationAmount = amount;
		return this;
	}
}
