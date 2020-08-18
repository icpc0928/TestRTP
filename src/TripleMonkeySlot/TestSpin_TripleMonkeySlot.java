package TripleMonkeySlot;

import com.gameTripleMonkeySlotLogic.sfs2x.TripleMonkeySlotLogic;

import java.util.HashMap;

public class TestSpin_TripleMonkeySlot {

    static int gameID = 163;
    static int lineBet = 1;
    static int testTimes = 100;

    static boolean isRateStatic = true;

    static TripleMonkeySlotLogic slot = new TripleMonkeySlotLogic(gameID);


    @SuppressWarnings("unchecked")
    public static void main(String[] args){
        double rate = 96;
        double prob = 96;

        slot.initGrid(prob);
//        boolean stop = true;
//        testTimes :
        for(int t = 0;t < testTimes  ;t++ )
        {

            HashMap<String, Object> resp = slot.getResult(lineBet, rate, prob);
            printHashMap(resp, t);
        }

    }

    public static void printHashMap(HashMap<String, Object> resp , int t){
        long totalWin = (long)resp.get("TotalWin");
        int useProb = (int)resp.get("UseProb");
        HashMap<String, Object> spins = (HashMap<String, Object>) resp.get("Spins");
        long totalBet = (long)resp.get("TotalBet");

        if(totalWin > 0)
        {
            System.out.println("======================="+t+"========================");
            printResp(totalWin, useProb, totalBet);
            for(int i = 0; i < spins.size(); i++){
                HashMap<String, Object> spin = (HashMap<String, Object>)spins.get(String.valueOf(i));
                int mode = (int)spin.get("Mode");
                int[][] grid = (int[][])spin.get("Grid");
                long roundBaseWin = (long)spin.get("RoundBaseWin");
                long roundTimesWin = (long)spin.get("RoundTimesWin");
                boolean[] isBonus = (boolean[])spin.get("IsBonus");
                int bonusTimes = (int)spin.get("BonusTimes");
                int nextBonusTimes = (int)spin.get("NextBonusTimes");
                boolean[] lookAt = (boolean[])spin.get("LookAt");
                boolean needReSpin = (boolean) spin.get("NeedReSpin");
                boolean[] isNextReelStop = (boolean[]) spin.get("IsNextReelStop");

                printSpin(mode, grid, roundBaseWin, roundTimesWin, isBonus, bonusTimes, nextBonusTimes, lookAt,needReSpin, isNextReelStop);
//                if(grid[0][0]==7 && (grid[1][0]==8 || grid[1][0] == 9) && (grid[2][0]==8 || grid[2][0] == 9) )
//                    stop = false;
            }
        }
    }

    private static void printResp(long totalWin, int useProb, long totalBet){
        System.out.println("TotalWin: " + totalWin);
        System.out.println("UseProb: " + useProb);
        System.out.println("TotalBet: " + totalBet + "\n");

    }

    private static void printSpin(int mode, int[][] grid, long roundBaseWin, long roundTimesWin, boolean[] isBonus, int bonusTimes,
                                  int nextBonusTimes, boolean[] lookAt, boolean needReSpin, boolean[] isNextReelStop){
        System.out.println("Mode: " + mode);
        System.out.println("RoundBaseWin: " + roundBaseWin);
        System.out.println("RoundTimesWin: " + roundTimesWin);
        System.out.println("BonusTimes: " + bonusTimes);
        System.out.println("NextBonusTimes: " + nextBonusTimes);
        System.out.println("NeedReSpin: " + needReSpin);
        System.out.print("IsNextReelStop: ");
        for(int i = 0; i < isNextReelStop.length;i++) {
            System.out.print(isNextReelStop[i]+", ");
        }
        System.out.print("\nIsBonus: ");
        for(int i = 0; i < isBonus.length;i++) {
            System.out.print(isBonus[i]+", ");
        }

        System.out.print("\nLookAt: ");
        for(int i = 0; i < lookAt.length;i++) {
            System.out.print(lookAt[i]+", ");
        }

        System.out.println();
        System.out.println("Grid: ");
        for(int i = 0; i < grid.length; i++){
            for(int j = 0; j < grid[i].length; j++){
                String result = (grid[j][i] < 10) ? "  "+grid[j][i] : " "+grid[j][i] ;
                System.out.print(result);
            }
            System.out.println();
        }

    }
}
