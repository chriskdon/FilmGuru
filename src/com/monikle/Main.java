package com.monikle;

import com.monikle.neuro.FeedForwardNetwork;
import com.monikle.neuro.NeuralNetwork;
import com.monikle.neuro.TrainerConfiguration;
import com.monikle.neuro.TrainingData;

public class Main {
	public static void main(String[] args) {
		NeuralNetwork network = new FeedForwardNetwork(3, 50, 2, 0.1, 0.9);

		TrainingData trainingData = new TrainingData();
		trainingData.add(new double[] {1, 2, 3}, new double[] {0.5, 1});
		trainingData.add(new double[] {1, 4, 3}, new double[] {0.2, 0.4});
		trainingData.add(new double[] {1, 2, 3}, new double[] {0.2, 1});
		trainingData.add(new double[] {1, 1, 3}, new double[] {0.5, 1});
		trainingData.add(new double[] {1, 5, 3}, new double[] {0.5, 1});

		TrainerConfiguration config = TrainerConfiguration.create(trainingData)
				.setMaxEpochs(100)
				.setAcceptableError(0.5)
				.setValidationAmount(0.2);

		network.train(config);

		System.out.println(network.run(1, 2, 3));
	}
}