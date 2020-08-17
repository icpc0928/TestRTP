package VarySlot;

import com.gameVarySlotLogic.sfs2x.VarySlotLogic;

import java.text.NumberFormat;
import java.util.HashMap;

public class TestRTP_VarySlot {


    static int gameID = 160;
    static int testTimes = 5000;
    static int lineBet = 1;
    static boolean isRateStatic = false;

    static VarySlotLogic slot = new VarySlotLogic(gameID);

    static int totalSpin;

    @SuppressWarnings("unchecked")
    public static void main(String[] args){
        double rate = 0;
        double prob = 96;

        long totalBet = slot.getTotalBet(lineBet);        //單次下注總額
        long totalWin;
        long totalBaseWin;              //單轉主遊戲
        long totalFreeWin;              //單轉免費遊戲
        long totalBonusWin;             //單轉Bonus

        double earn;                    //單次淨利
        double[] earnList = new double[testTimes];
        double earnStandDev;    //淨利標準差

        long finalBet = 0;
        long finalWin = 0;
        long finalBaseWin = 0;
        long finalFreeWin = 0;
        long finalBonusWin = 0;         //bonusAward

        int countTotalWin = 0;          //計算單將有贏錢的次數
        int countBaseWin = 0;

        int countBonus = 0;             //計算進入bonus次數
        int countBonusWin = 0;	        //計算bonus 有中獎次數

        int countFreeSpin = 0;	        //免費遊戲總轉次
        int countFreeSpinWin = 0;       //計算單轉免費遊戲中獎次數


        int countWin10To30 = 0;
        int countWin30To50 = 0;
        int countWin50Up = 0;

        long maxTotalWin = 0;	        //投注一次最大獲利金額
        long maxSpinWin = 0;	        //單轉一次最大獲利金額
        long maxBonusWin = 0;           //小遊戲單次最大獲利金額

        int[][] maxTotalWinGrid = null; //單局最高贏分圖示表(主遊戲)
        int[][] maxSpinWinGrid = null;	//單轉最高贏分圖示表(主遊戲||免費遊戲)




        slot.initGrid(prob);

        for(int i = 0; i < testTimes; i++)
        {
            System.out.println("=========================< "+i+" >=========================");
            finalBet += totalBet;   //付錢
            earn = 0 - totalBet;    //本次淨利

            //初始化
            totalWin = 0;
            totalBaseWin = 0;
            totalFreeWin = 0;
            totalBonusWin = 0;


            //主遊戲-->
            HashMap<String, Object> resp = slot.getResult(lineBet, rate, prob);
            HashMap<String, Object> spinMap = (HashMap<String, Object>) resp.get("Spin");

            HashMap<String, Object> oneSpin ;
            //Spin 0~10
            for(int j = 0; j < spinMap.size(); j++)
            {
                oneSpin = (HashMap<String, Object>)spinMap.get(String.valueOf(j));

                //主遊戲
                if(j == 0)
                {
                    totalBaseWin = (long)oneSpin.get("TotalWin");
                    finalBaseWin += totalBaseWin;

                    if(totalBaseWin > 0) countBaseWin++;
                    if(totalBaseWin > maxSpinWin){
                        maxSpinWin = totalBaseWin;
                        maxSpinWinGrid = (int[][])oneSpin.get("Grid");
                    }
                    maxSpinWin = Math.max(totalBaseWin, maxSpinWin);

                    //Bonus轉盤
                    if((boolean)oneSpin.get("Bonus"))
                    {
                        HashMap<String, Object> bonus = (HashMap<String, Object>) resp.get("Bonus");
                        countBonus ++;
                        totalBonusWin = (long)bonus.get("BonusAward");
                        finalBonusWin += totalBonusWin;
                        if(totalBonusWin > 0) countBonusWin++;
                        maxBonusWin = Math.max(maxBonusWin, totalBonusWin);
                    }
                }
                //免費遊戲 (每一轉)
                else
                {
                    countFreeSpin++;
                    totalFreeWin = (long)oneSpin.get("TotalWin");
                    finalFreeWin += totalFreeWin;

                    if(totalFreeWin > 0) countFreeSpinWin++;
                    if(totalFreeWin > maxSpinWin){
                        maxSpinWin = totalFreeWin;
                        maxSpinWinGrid = (int[][])oneSpin.get("Grid");
                    }
                }
            }


            //本局遊戲結束
            //計算RTP
            rate = (isRateStatic) ? rate : (( (double)finalWin / (double) finalBet ) * 100);

            //計算本局遊戲數據-->
            totalWin = (long)resp.get("TotalWin");
            finalWin += totalWin;

            //檢查加總是否正確
            if(!(finalWin == finalBaseWin + finalBonusWin + finalFreeWin)){
                System.out.println("加總錯誤");
                break;
            }

            if(totalWin > 0)
            {
                countTotalWin++;
                if(totalWin > maxTotalWin){
                    maxTotalWin = totalWin;
                    oneSpin = (HashMap<String, Object>) spinMap.get("0");
                    maxTotalWinGrid = (int[][]) oneSpin.get("Grid");
                }
                if((totalWin / totalBet) > 50) countWin50Up++;
                else if((totalWin / totalBet > 30)) countWin30To50++;
                else if((totalWin / totalBet > 10)) countWin10To30++;
            }
            earn = totalWin - totalBet;
            earnList[i] = earn;
            //<--計算本局遊戲數據
        }
        //迴圈結束處理數據
        rate = ((double)finalWin / (double)finalBet) *100;
        totalSpin = testTimes + countFreeSpin + countBonus;

        //迴圈結束印出所有數據-->
        showGameInfo(gameID, testTimes);
        showGameRTP(finalBet, finalWin, finalBaseWin, finalFreeWin, finalBonusWin, rate);
        showGameCount(totalSpin, testTimes, countFreeSpin, countBonus, finalFreeWin, finalBonusWin);
        showOdds(totalSpin,testTimes,countFreeSpin, countBonus, countBaseWin, countFreeSpinWin,
                countBonusWin, finalWin, finalBaseWin, finalFreeWin, finalBonusWin, totalBet, earnList);
        showAward(totalSpin, countWin10To30, countWin30To50, countWin50Up);
        showMaxWin(totalBet, maxTotalWin, maxSpinWin, maxBonusWin, maxTotalWinGrid, maxSpinWinGrid);
        showStandardDeviation(earnList, totalBet);



    }

    public static void showGameInfo(int gameID, int testTimes){
        System.out.println("測試遊戲： " + gameID);
        System.out.println("測試次數： " + testTimes);
    }

    public static void showGameRTP(long finalBet, long finalWin, long finalBaseWin, long finalFreeWin, long finalBonusWin, double rate){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        double baseRTP = ((double)finalBaseWin / (double)finalBet) * 100;
        double freeRTP = ((double)finalFreeWin / (double)finalBet) * 100;
        double bonusRTP = ((double)finalBonusWin / (double)finalBet) * 100;

        System.out.println("============== 總獲利 =============="); //28個=
        System.out.println("總押分　　　: " + finalBet);
        System.out.println("總贏分　　　: " + finalWin);
        System.out.println("主遊戲贏分　: " + finalBaseWin);
        System.out.println("免費遊戲贏分: " + finalFreeWin);
        System.out.println("小遊戲贏分　: " + finalBonusWin);

        System.out.println("=============== RTP ==============="); //30個=
        System.out.println("整體　　: " + nf.format(rate));
        System.out.println("主遊戲　: " + nf.format(baseRTP));
        System.out.println("免費遊戲: " + nf.format(freeRTP));
        System.out.println("小遊戲　: " + nf.format(bonusRTP));
    }

    public static void showGameCount(int totalSpin, int testTimes, int countFreeSpin, int countBonus, long finalFreeWin, long finalBonusWin){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        double averageBaseToFree = (double)testTimes / ((double)countFreeSpin / 10);        //平均主中免費遊戲局數
        double averageBaseToBonus = (double)testTimes / (double)countBonus;                 //平均主中小遊戲局數
        double averageBonusWin = (double)finalBonusWin / (double)countBonus;                //平均小遊戲贏分
        double averageBonusToFree = (double)countBonus / ((double)countFreeSpin / 10);      //平均小遊戲中免費遊戲局數
        double averageFreeWin = (double)finalFreeWin / ((double)countFreeSpin / 10);        //每局免費遊戲平均贏分(10局)
        double averageFreeWinInSpin = (double)finalFreeWin / (double)countFreeSpin;         //平均每轉免費遊戲贏分

        System.out.println("========== 局數及中獎次數 ===========");
        System.out.println("總局數: " + totalSpin);
        System.out.printf("主遊戲: %d; 免費遊戲: %d; 小遊戲: %d;\n", testTimes, countFreeSpin, countBonus );
        System.out.println("平均主中小局數　: " + nf.format(averageBaseToBonus));
        System.out.println("平均小中免局數　: " + nf.format(averageBonusToFree));
        System.out.println("平均主中免局數　: " + nf.format(averageBaseToFree));
        System.out.println("小遊戲平均贏分　: " + nf.format(averageBonusWin));
        System.out.println("免費遊戲平均贏分: " + nf.format(averageFreeWin));
        System.out.println("平均每轉免費贏分: " + nf.format(averageFreeWinInSpin));
    }

    public static void showOdds(int totalSpin, int testTimes, int countFreeSpin, int countBonus, int countBaseWin, int countFreeSpinWin, int countBonusWin,
                                long finalWin, long finalBaseWin, long finalFreeWin, long finalBonusWin, long totalBet, double[] earnList){

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);

        int totalWinTimes = countBaseWin + countFreeSpinWin + countBonusWin;
        double totalOddsOfTotalSpin = (double)totalWinTimes / (double)totalSpin;    //總中獎率

        double baseOddsOfTotalSpin = (double)countBaseWin / (double)totalSpin;      //總體-主遊戲中獎率
        double freeOddsOfTotalSpin = (double)countFreeSpinWin / (double)totalSpin;  //總體-免費遊戲中獎率
        double bonusOddsOfTotalSpin = (double)countBonusWin / (double)totalSpin;    //總體-小遊戲中獎率

        double baseOddsOfBaseSpin = (double)countBaseWin / (double)testTimes;       //個別-主遊戲中獎率
        double freeOddsOfFreeSpin = (countFreeSpin == 0) ? 0 : (double)countFreeSpinWin / (double)countFreeSpin;    //個別-免費遊戲中獎率
        double bonusOddsOfBonusSpin = (countBonusWin == 0) ? 0 : (double)countBonusWin / (double)countBonus;     //個別-小遊戲中獎率
        double totalWinOfWinTimes = (totalWinTimes == 0) ? 0 : (double)totalSpin / (double)totalWinTimes;           //總贏分次數平均局數(總轉數/總贏分次數)

        double averageWinTotalGame = (totalWinTimes == 0) ? 0 : ((double)finalWin / (double)totalWinTimes);
        double averageWinBaseGame = (countBaseWin == 0) ? 0 : (double)finalBaseWin / (double)countBaseWin;
        double averageWinFreeGame = (countFreeSpinWin == 0) ? 0 : (double)finalFreeWin / (double)countFreeSpinWin;
        double averageWinBonusGame = (countBonusWin == 0) ? 0 : (double)finalBonusWin / (double)countBonusWin;

        int countEarnTimes = returnEarnTimes(earnList);
        double totalEarn = returnTotalEarn(earnList);


        System.out.println("============== 中獎率 ==============");
        System.out.println("總中獎率: " + nf.format(totalOddsOfTotalSpin));
        System.out.println("--------- 總體中獎率 ---------");
        System.out.println("主遊戲　: " + nf.format(baseOddsOfTotalSpin)); 		//主遊戲中獎次數 / totalSpin
        System.out.println("免費遊戲: " + nf.format(freeOddsOfTotalSpin)); 		//免費遊戲中獎次 / totalSpin
        System.out.println("小遊戲　: " + nf.format(bonusOddsOfTotalSpin));		//小遊戲中獎次數 / totalSpin
        System.out.println("--------- 個別中獎率 ---------");
        System.out.println("主遊戲　: " + nf.format(baseOddsOfBaseSpin)); 		//主遊戲中獎次數 / testTimes
        System.out.println("免費遊戲: " + nf.format(freeOddsOfFreeSpin)); 		//免費遊戲中獎次 / countFreeSpin
        System.out.println("小遊戲　: " + nf.format(bonusOddsOfBonusSpin));		//小遊戲中獎次數 / countBonusWin

        System.out.println("============ 贏分次數計算 ============");
        System.out.println("總贏分次數: " + totalWinTimes); // totalWinTimes
        System.out.println("總贏分次數平均局數(總轉數/總贏分次數): " + nf.format(totalWinOfWinTimes));//總轉數(totalSpin) / totalWinTimes
        System.out.println("總贏分次數平均贏分: " + nf.format(averageWinTotalGame));
        System.out.println("--------- 淨利次數計算 ---------");
        System.out.println("總淨利次數: " + countEarnTimes); // earnList[i] > 0
        System.out.println("總淨利平均次數(主遊戲/淨利次數): " + nf.format((double)testTimes / (double)countEarnTimes));//主遊戲次數(testTimes) / 總淨利次數
        System.out.println("總淨利次數平均贏分: " + nf.format(totalEarn / (double)countEarnTimes)); // sum earnList[i]>0 / 總淨利次數
        System.out.println("----------- 主遊戲 ------------");
        System.out.println("主遊戲贏分次數: " + countBaseWin);
        System.out.println("主遊戲贏分次數平均局數: " + nf.format((double)testTimes / (double)countBaseWin)); //testTimes / countBaseWin
        System.out.println("主遊戲贏分次數平均贏分: " + nf.format(averageWinBaseGame));
        System.out.println("主遊戲贏分次數平均贏分倍數: " + nf.format(averageWinBaseGame / (double)totalBet)); //averageWinBaseGame / totalBet
        System.out.println("----------- 免費遊戲 -----------");
        System.out.println("免費遊戲贏分次數: " + countFreeSpinWin);
        System.out.println("免費遊戲贏分次數平均局數: " + nf.format((double)countFreeSpin / (double)countFreeSpinWin));
        System.out.println("免費遊戲贏分次數平均贏分: " + nf.format(averageWinFreeGame));
        System.out.println("免費遊戲贏分次數平均贏分倍數: " + nf.format(averageWinFreeGame / (double)totalBet));
        System.out.println("----------- 小遊戲 ------------");
        System.out.println("小遊戲贏分次數: " + countBonusWin);
        System.out.println("小遊戲贏分次數平均局數: " + nf.format((double)countBonus / (double)countBonusWin ));
        System.out.println("小遊戲贏分次數平均贏分: " + nf.format(averageWinBonusGame));
        System.out.println("小遊戲贏分次數平均贏分倍數: " + nf.format(averageWinBonusGame / (double)totalBet));

    }

    public static void showAward(int totalSpin, int countWin10To30, int countWin30To50, int countWin50Up){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        int countWin10Up = countWin10To30 + countWin30To50 + countWin50Up;
        System.out.println("============ 大獎次數統計 ============");
        System.out.println("總大獎次數: " + countWin10Up);
        System.out.println("大獎次數　: " + countWin10To30);
        System.out.println("大獎中獎率: " + nf.format((double)totalSpin / (double)countWin10To30));	//總轉次 / 大獎次數
        System.out.println("巨獎次數　: " + countWin30To50);
        System.out.println("巨獎中獎率: " + nf.format((double)totalSpin / (double)countWin30To50));
        System.out.println("超獎次數　: " + countWin50Up);
        System.out.println("超獎中獎率: " + nf.format((double)totalSpin / (double)countWin50Up));
    }

    public static void showMaxWin(long totalBet, long maxTotalWin, long maxSpinWin,long maxBonusWin, int[][] maxTotalWinGrid, int[][] maxSpinWinGrid){
        System.out.println("============== 最高分 ==============");
        System.out.println("單局最高贏分: " + maxTotalWin);
        System.out.println("單局最高贏分倍數: " + ((double)maxTotalWin / (double)totalBet));
        System.out.println("單局最高組合如下");
        printGrid(maxTotalWinGrid);
        System.out.println();
        System.out.println("單轉最高贏分: " + maxSpinWin);
        System.out.println("單轉最高贏分倍數: " + ((double)maxSpinWin / (double)totalBet));
        System.out.println("單轉最高組合如下");
        printGrid(maxSpinWinGrid);
        System.out.println();
        System.out.println("小遊戲最高贏分: " + maxBonusWin);
        System.out.println("小遊戲最高贏分倍數: " + ((double)maxBonusWin / (double)totalBet));
        System.out.println();
    }

    private static int returnEarnTimes(double[] earnList){
        int result =0;
        for(int i = 0; i < earnList.length; i++){
            if(earnList[i] > 0) result++;
        }
        return result;
    }

    private static double returnTotalEarn(double[] earnList){
        double totalEarn = 0;
        for(int i = 0; i < earnList.length; i++){
            if(earnList[i] > 0) totalEarn += earnList[i];
        }
        return totalEarn;
    }

    private static void printGrid(int[][] maxSpinWinGrid) {

        for(int i = 0; i < maxSpinWinGrid[0].length; i++){
            for(int j = 0; j < maxSpinWinGrid.length; j++){
                String result = (maxSpinWinGrid[j][i] < 10) ? "  "+maxSpinWinGrid[j][i] : " "+maxSpinWinGrid[j][i] ;
                System.out.print(result);
            }
            System.out.println();
        }
    }

    public static void showStandardDeviation(double[] earnList, long totalBet){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(6);
        long sum = 0;
        double average;
        double deviation;
        double sumDeviation = 0;
        double result;
        for(int i = 0; i < earnList.length; i++){
            sum += earnList[i];
        }
        average = sum / earnList.length;
        for(int i = 0; i < earnList.length; i++){
            deviation = Math.pow((earnList[i] - average), 2);
            sumDeviation += deviation;
        }
        result = Math.sqrt(sumDeviation / earnList.length) / (double)totalBet;
        System.out.println("============標準差============");
        System.out.println("輸贏標準差: " + nf.format(result));
    }



}
