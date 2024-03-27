package src.pas.battleship.agents;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SYSTEM IMPORTS


// JAVA PROJECT IMPORTS
import edu.bu.battleship.agents.Agent;
import edu.bu.battleship.game.Game.GameView;
import edu.bu.battleship.game.ships.Ship.ShipType;
import edu.bu.battleship.game.EnemyBoard;
import edu.bu.battleship.game.EnemyBoard.Outcome;
import edu.bu.battleship.utils.Coordinate;
import src.pas.battleship.agents.ProbabilisticAgent.BattleshipProbabilityCalculatorSeparated;


public class ProbabilisticAgent
    extends Agent
{

    public ProbabilisticAgent(String name)
    {
        super(name);
        System.out.println("[INFO] ProbabilisticAgent.ProbabilisticAgent: constructed agent");
    }

    public boolean first_move = true;
    int gridX = -1;
    int gridY =-1;
    int[][] newgrid = new int[10][10];

    @Override
    public Coordinate makeMove(final GameView game)
    {

        EnemyBoard.Outcome[][] outcomes = game.getEnemyBoardView();
        // Assuming Outcome is an enum or class that has a meaningful toString() method

   

        
        BattleshipProbabilityCalculatorSeparated calculator = new BattleshipProbabilityCalculatorSeparated();
        int[] shipSizes = getShipSizesArray(game.getEnemyShipTypeToNumRemaining()); // Example ship size
        int gridWidth = game.getGameConstants().getNumRows();
        int gridHeight = game.getGameConstants().getNumCols();
        
        // Calculate the probability grid
        int[][] probabilityGrid = calculator.calculateProbabilityGrid(shipSizes, gridWidth, gridHeight);
        if (!first_move) {
            probabilityGrid = newgrid;
        }

        System.out.println(gridX);
        System.out.println(gridY);
        System.out.println(probabilityGrid);
        
        if (first_move == false && outcomes[gridX][gridY] == EnemyBoard.Outcome.MISS ){
        this.updateGridAfterMissConsideringShipSize(probabilityGrid, gridX, gridY, shipSizes, gridWidth, gridHeight);
        }

        if (first_move == false && outcomes[gridX][gridY] == EnemyBoard.Outcome.HIT ){
            probabilityGrid[gridX][gridY] = 0;
            this.updateGridAfterHit(probabilityGrid, gridX, gridY, gridWidth, gridHeight, 20);
            }




        // Find and return the cells with the highest probability
        List<int[]> maxProbCells = calculator.findMaxProbabilityCells(probabilityGrid);
        if (!maxProbCells.isEmpty()) {
            int[] firstMaxCell = maxProbCells.get(0);
            gridX = firstMaxCell[0]; // x-coordinate of the first cell
            gridY = firstMaxCell[1]; // y-coordinate of the first cell
        }
        
        Coordinate result = new Coordinate(gridX, gridY);
        first_move = false;
        newgrid = probabilityGrid;
        return result;
    }


    public static class BattleshipProbabilityCalculatorSeparated {

    int gridWidth;
    int gridHeight;
    // Calculates and returns the probability grid
    public int[][] calculateProbabilityGrid(int[] shipSizes, int gridWidth, int gridHeight) {
        int[][] probabilityGrid = new int[gridWidth][gridHeight];
        
        for (int shipSize : shipSizes) {
            // Horizontal placements
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x <= gridWidth - shipSize; x++) {
                    for (int i = 0; i < shipSize; i++) {
                        probabilityGrid[x + i][y]++;
                    }
                }
            }

            // Vertical placements
            for (int x = 0; x < gridWidth; x++) {
                for (int y = 0; y <= gridHeight - shipSize; y++) {
                    for (int i = 0; i < shipSize; i++) {
                        probabilityGrid[x][y + i]++;
                    }
                }
            }
        }
        
        return probabilityGrid;
    }

    // Finds and returns the cells with the highest probability
    public List<int[]> findMaxProbabilityCells(int[][] probabilityGrid) {
        List<int[]> maxProbCells = new ArrayList<>();
        int maxProbability = 0;
        
        for (int x = 0; x < probabilityGrid.length; x++) {
            for (int y = 0; y < probabilityGrid[x].length; y++) {
                int currentProbability = probabilityGrid[x][y];
                if (currentProbability > maxProbability) {
                    maxProbability = currentProbability;
                    maxProbCells.clear();
                    maxProbCells.add(new int[]{x, y});
                } else if (currentProbability == maxProbability) {
                    maxProbCells.add(new int[]{x, y});
                }
            }
        }
        
        return maxProbCells;
    }





}
public int[] getShipSizesArray(Map<ShipType, Integer> shipsRemaining) {
        List<Integer> shipSizesList = new ArrayList<>();
        
        for (Map.Entry<ShipType, Integer> entry : shipsRemaining.entrySet()) {
            int shipSize = getShipSize(entry.getKey());
            for (int i = 0; i < entry.getValue(); i++) {
                shipSizesList.add(shipSize);
            }
        }
        
        // Convert the List to an array
        int[] shipSizesArray = shipSizesList.stream().mapToInt(i -> i).toArray();
        
        return shipSizesArray;
    }
    
    private int getShipSize(ShipType shipType) {
        switch (shipType) {
            case AIRCRAFT_CARRIER:
                return 5;
            case BATTLESHIP:
                return 4;
            case DESTROYER:
            case SUBMARINE:
                return 3;
            case PATROL_BOAT:
                return 2;
            default:
                throw new IllegalArgumentException("Unknown ship type: " + shipType);
        }
    }

    public void updateGridAfterMissConsideringShipSize(int[][] probabilityGrid, int missX, int missY, int[] shipSizes, int gridWidth, int gridHeight) {
        // Mark the missed cell to ignore in future calculations
        probabilityGrid[missX][missY] = 0;
    
        // Temporarily store adjustments to avoid modifying the grid in-place while iterating
        int[][] adjustments = new int[gridWidth][gridHeight];
    
        // Check each ship size for potential placements
        for (int size : shipSizes) {
            // Horizontal ship placement adjustments
            for (int x = Math.max(0, missX - (size - 1)); x <= Math.min(gridWidth - 1, missX + (size - 1)); x++) {
                if (x + size <= gridWidth) { // Check if ship can be placed starting at x
                    for (int i = 0; i < size; i++) {
                        // Decrease probability for segments that could include the missed cell
                        if (x + i == missX) {
                            for (int j = 0; j < size; j++) {
                                if (x + j < gridWidth) {
                                    adjustments[x + j][missY] -= 1;
                                }
                            }
                        }
                    }
                }
            }
    
            // Vertical ship placement adjustments
            for (int y = Math.max(0, missY - (size - 1)); y <= Math.min(gridHeight - 1, missY + (size - 1)); y++) {
                if (y + size <= gridHeight) { // Check if ship can be placed starting at y
                    for (int i = 0; i < size; i++) {
                        // Decrease probability for segments that could include the missed cell
                        if (y + i == missY) {
                            for (int j = 0; j < size; j++) {
                                if (y + j < gridHeight) {
                                    adjustments[missX][y + j] -= 1;
                                }
                            }
                        }
                    }
                }
            }
        }
    
        // Apply adjustments to the probability grid
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                probabilityGrid[x][y] += adjustments[x][y];
                // Ensure probabilities do not become negative
                if (probabilityGrid[x][y] < 0) {
                    probabilityGrid[x][y] = 0;
                }
            }
        }
    }

    public void updateGridAfterHit(int[][] probabilityGrid, int hitX, int hitY, int gridWidth, int gridHeight, int increaseAmount) {
        // Increase the probability of cardinally adjacent cells
        // Up
        if (hitY > 0) {
            probabilityGrid[hitX][hitY - 1] += increaseAmount;
        }
        // Down
        if (hitY < gridHeight - 1) {
            probabilityGrid[hitX][hitY + 1] += increaseAmount;
        }
        // Left
        if (hitX > 0) {
            probabilityGrid[hitX - 1][hitY] += increaseAmount;
        }
        // Right
        if (hitX < gridWidth - 1) {
            probabilityGrid[hitX + 1][hitY] += increaseAmount;
        }
        
        // Optional: Cap the probabilities to a maximum value if necessary
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (probabilityGrid[x][y] > 100) { // Assuming 100 is the max probability for illustrative purposes
                    probabilityGrid[x][y] = 100;
                }
            }
        }
    }


    
    
    @Override
    public void afterGameEnds(final GameView game) {}

}
