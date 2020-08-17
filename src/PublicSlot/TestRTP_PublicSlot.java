package PublicSlot;

import com.gamePubSlotLogic.sfs2x.PubSlotLogic;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TestRTP_PublicSlot {

    //PublicSlot: 23.24.31.32.33.38.39.43.44.45.46.47.48.49.50.51.52.53.95.96

    static int gameID = 71;
    static int testTimes = 100000000;
    static int lineBet = 1;		//線注金額
    static boolean isRateStatic = false;  //測試表格真實RTP， true: rate永遠為固定給的值，false: rate隨投注變動

    static PubSlotLogic slot = new PubSlotLogic(gameID);

    static int totalSpin;


    public static void main(String[] args) {
//		PubSlotLogic slot = new PubSlotLogic(gameID);


        double rate = 0;
        double prob = 96;

        long totalBet = lineBet * slot.getTotalBet(lineBet);	//單次下注總額
        long totalWin;
        long totalBaseWin;
        long totalFreeWin;
        long totalBonusWin;

        double earn;			//單次淨利
        double[] earnList = new double[testTimes];
        double earnStandDev;	//淨利標準差

        long finalBet = 0;
        long finalWin = 0;
        long finalBaseWin = 0;
        long finalFreeWin = 0;
        long finalBonusWin = 0;

        int countTotalWin = 0;
        int countBaseWin = 0;
        int countFreeWin = 0;
        int countFreeSpinWin = 0; //計算單轉免費遊戲中獎次數
        int countBonusWin = 0;	//因一定會中獎 故=中小遊戲次數

        int countWin10To30 = 0;
        int countWin30To50 = 0;
        int countWin50Up = 0;

        long maxTotalWin = 0;	//投注一次最大獲利金額
        long maxSpinWin = 0;	//單轉一次最大獲利金額
        int[][] maxTotalWinGrid = null;//單局最高贏分圖示表
        int[][] maxSpinWinGrid = null;	//單轉最高贏分圖示表

        int countFreeSpin = 0;	//免費遊戲總轉次(含主中免 + 免中免)
        ArrayList<Integer> countBaseToFreeType = new ArrayList<>(4);	//主中免次數分類
        countBaseToFreeType.add(0);countBaseToFreeType.add(0);countBaseToFreeType.add(0);countBaseToFreeType.add(0);
        ArrayList<Integer> countFreeToFreeType = new ArrayList<>(4);	//免中免次數分類
        countFreeToFreeType.add(0);countFreeToFreeType.add(0);countFreeToFreeType.add(0);countFreeToFreeType.add(0);



        int opt = 0;	//Bonus 用的opt


        slot.initGrid(prob);

        for(int i = 0; i < testTimes; i++){
            System.out.println("=========================< "+i+" >=========================");
            finalBet += totalBet;	//付錢
            earn = 0 - totalBet;	//本次淨利

            //初始化
            totalWin = 0;
            totalBaseWin = 0;
            totalFreeWin = 0;
            totalBonusWin = 0;



            //主遊戲-->
            HashMap<String,Object> respBase = slot.getBaseResult(lineBet, rate, prob);
            totalBaseWin = (long)respBase.get("TotalWin");
            finalBaseWin += totalBaseWin;
            finalWin += totalBaseWin;

            //計算主遊戲中獎次數
            if(totalBaseWin > 0) countBaseWin++;
            if(totalBaseWin > maxSpinWin){
                maxSpinWin = totalBaseWin;
                maxSpinWinGrid = (int[][])respBase.get("Grid");
            }
            maxSpinWin = Math.max(totalBaseWin, maxSpinWin);

            //主遊戲後 rate
            rate = (isRateStatic) ? rate : (( (double)finalWin / (double)finalBet ) * 100) ;


            //<--主遊戲結束
            //免費遊戲-->
            if((int)respBase.get("FreeSpinCount") > 0){
                int countDownFree = (int)respBase.get("FreeSpinCount");
                countBaseToFreeType = classifyFreeGame(countBaseToFreeType, countDownFree, false);		//分類 主中免 次數列表

                //免費遊戲次數迴圈
                for(int j = 0; j < countDownFree; j++){
                    HashMap<String, Object> respFree = slot.getFreeResult(lineBet, rate, prob);

                    //免費遊戲 中 免費遊戲-->
                    if((int)respFree.get("FreeSpinCount") > 0){
                        countDownFree += (int)respFree.get("FreeSpinCount");
                        countFreeToFreeType = classifyFreeGame(countFreeToFreeType, (int)respFree.get("FreeSpinCount"), true); //分類 免中免 次數列表
                    }

                    if((long)respFree.get("TotalWin") > maxSpinWin){
                        maxSpinWin = (long)respFree.get("TotalWin");
                        maxSpinWinGrid = (int[][])respFree.get("Grid");
                    }

                    if((long)respFree.get("TotalWin") > 0) countFreeSpinWin++; //計算單轉有獲利..用於計算免費轉數的中獎率

                    totalFreeWin += (long)respFree.get("TotalWin");
                    finalWin += (long)respFree.get("TotalWin");
                    //免費遊戲後 rate
                    rate =(isRateStatic) ? rate :  (( (double)finalWin / (double)finalBet) * 100);
                }
                if(totalFreeWin > 0) countFreeWin++;
                finalFreeWin += totalFreeWin;
            }//<--免費遊戲結束

            //小遊戲-->
            if((boolean)respBase.get("Bonus")){
                countBonusWin ++;
                HashMap<String, Object> respBonus = slot.getBonusResult(lineBet, opt, rate, prob);

                totalBonusWin = (long)respBonus.get("TotalWin");
                finalBonusWin += totalBonusWin;
                finalWin += totalBonusWin;

                maxSpinWin = Math.max(maxSpinWin, totalBonusWin);
                //小遊戲後 rate
                rate = (isRateStatic) ? rate : (( (double)finalWin / (double)finalBet ) * 100);
            }//<--小遊戲結束


            //本次遊戲結束
            //計算本局遊戲數據-->
            totalWin = totalBaseWin + totalFreeWin + totalBonusWin;

            if(totalWin > 0){
                countTotalWin ++;								//紀錄中獎次數
                if(totalWin > maxTotalWin){
                    maxTotalWin = totalWin;
                    maxTotalWinGrid = (int[][])respBase.get("Grid");
                }
                if( (totalWin / totalBet) > 50) countWin50Up++;			//超級巨獎次數
                else if( (totalWin / totalBet) > 30) countWin30To50++;	//巨獎次數
                else if( (totalWin / totalBet) > 10) countWin10To30++;	//大獎次數
            }
            earn = totalWin - totalBet;							//本次總淨利(損)
            earnList[i] = earn;									//輸入至淨利列表




            //<--計算本局遊戲數據結束
        }
        //迴圈結束處理數據-->
        rate = ((double)finalWin / (double)finalBet) * 100;
        countFreeSpin = returnFreeGameTimes(countBaseToFreeType, countFreeToFreeType);
        totalSpin = testTimes + countFreeSpin + countBonusWin;


        //迴圈結束印出所有數據-->
        showGameInfo(gameID, testTimes);
        showGameRTP(finalBet, finalWin, finalBaseWin, finalFreeWin, finalBonusWin, rate);
        showGameCount(totalSpin, testTimes, countFreeSpin, countBaseToFreeType, countFreeToFreeType, countBonusWin, finalFreeWin);
        showOdds(totalSpin, testTimes, countFreeSpin, countBonusWin, countBaseWin, countFreeWin, countFreeSpinWin, finalWin, finalBaseWin, finalFreeWin, finalBonusWin, totalBet, earnList);
        showAward(totalSpin, countWin10To30, countWin30To50, countWin50Up);
        showMaxWin(totalBet, maxTotalWin, maxSpinWin, maxTotalWinGrid, maxSpinWinGrid);
        showStandardDeviation(earnList, totalBet);






    }

    //分類 主中免 / 免中免 的方法， 將回傳計算完成的ArrayList {總次數, 10次, 20次, 30次}
    public static ArrayList<Integer> classifyFreeGame(ArrayList<Integer> countFreeSpinType, int freeSpinCount, boolean isFreeToFree){
        int maxReel = slot.getMaxReel();
        int[] freeCountArray = (isFreeToFree) ? slot.getFreeToFreeCount() : slot.getFreeCount(); 	 	//[0,0,0,10,20,30]
        int index = Arrays.binarySearch(freeCountArray, freeSpinCount) - 2;

        countFreeSpinType.set(0, countFreeSpinType.get(0) + 1);
        countFreeSpinType.set(index, countFreeSpinType.get(index) + 1);
        return countFreeSpinType;
    }

    //回傳總免費轉數
    public static int returnFreeGameTimes(ArrayList<Integer> countBaseToFreeType, ArrayList<Integer> countFreeToFreeType){
        int maxReel = slot.getMaxReel();  //5軸
        int result = 0;		//總免費轉數
        int[] baseToFreeArray = slot.getFreeCount();		//[0,0,0,10,20,30]
        int[] freeToFreeArray = slot.getFreeToFreeCount();	//[0,0,0,10,20,30]

        for(int i = 0; i < 3; i++){
            result += countBaseToFreeType.get(i + 1) * baseToFreeArray[i + (maxReel - 2)];
            result += countFreeToFreeType.get(i + 1) * freeToFreeArray[i + (maxReel - 2)];
        }
        return result;
    }

    public static void showGameInfo(int gameID, int testTimes){
        System.out.println("測試遊戲: " + gameID);
        System.out.println("測試次數: " + testTimes);

    }

    public static void showGameRTP(long finalBet, long finalWin,long finalBaseWin, long finalFreeWin, long finalBonusWin, double rate){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        double baseRTP = ( (double)finalBaseWin / (double)finalBet) * 100;
        double freeRTP = ( (double)finalFreeWin / (double)finalBet) * 100;
        double bonusRTP = ( (double)finalBonusWin / (double)finalBet) * 100;

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

    public static void showGameCount(int totalSpin, int testTimes, int countFreeSpin, ArrayList<Integer> countBaseToFreeType, ArrayList<Integer> countFreeToFreeType, int countBonusWin , long finalFreeWin){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        double averageBaseToFree 	= (double)testTimes / (double)countBaseToFreeType.get(0);
        double averageFreeToFree 	= (double)countBaseToFreeType.get(0) / (double)countFreeToFreeType.get(0);
        double averageFreeWin 		= (double)finalFreeWin / (double)countBaseToFreeType.get(0);
        double averageFreeWinInSpin = (double)finalFreeWin / (double)countFreeSpin;
        double averageFreeSpin 		= (double)countFreeSpin/ (double)countBaseToFreeType.get(0);


        System.out.println("========== 局數及中獎次數 ===========");
        System.out.println("總局數: " + totalSpin);
        System.out.printf("主遊戲: %d; 免費遊戲: %d; 小遊戲: %d;\n", testTimes, countFreeSpin, countBonusWin );
        System.out.println("中免費遊戲總次數: " + (countBaseToFreeType.get(0) + countFreeToFreeType.get(0)));
        System.out.printf("主中免次數: %d = (%d)+(%d)+(%d) \n", countBaseToFreeType.get(0), countBaseToFreeType.get(1), countBaseToFreeType.get(2), countBaseToFreeType.get(3));
        System.out.printf("免中免次數: %d = (%d)+(%d)+(%d) \n", countFreeToFreeType.get(0), countFreeToFreeType.get(1), countFreeToFreeType.get(2), countFreeToFreeType.get(3));
        System.out.println("平均主中免局數　: " + nf.format(averageBaseToFree));
        System.out.println("平均免中免局數　: " + nf.format(averageFreeToFree));
        System.out.println("免費遊戲平均贏分: " + nf.format(averageFreeWin));
        System.out.println("平均每轉免費贏分: " + nf.format(averageFreeWinInSpin));
        System.out.println("平均主中免能轉　: " + nf.format(averageFreeSpin));




    }

    public static void showOdds(int totalSpin, int testTimes, int countFreeSpin, int countBonusWin, int countBaseWin, int countFreeWin, int countFreeSpinWin,
                                long finalWin, long finalBaseWin, long finalFreeWin, long finalBonusWin, long totalBet, double[] earnList){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);

        int totalWinTimes = countBaseWin + countFreeSpinWin + countBonusWin;
        double totalOddsOfTotalSpin = (double)totalWinTimes / (double)totalSpin;	//總中獎率

        double baseOddsOfTotalSpin = (double)countBaseWin / (double)totalSpin;		//總體-主遊戲中獎率
        double freeOddsOfTotalSpin = (double)countFreeSpinWin / (double)totalSpin;	//總體-免費遊戲中獎率
        double bonusOddsOfTotalSpin = (double)countBonusWin / (double)totalSpin;	//總體-小遊戲中獎率

        double baseOddsOfBaseSpin = (double)countBaseWin / (double)testTimes;
        double baseOddsOfFreeSpin = (countFreeSpin == 0) ? 0 : (double)countFreeSpinWin / (double)countFreeSpin;
        double baseOddsOfBonusSpin = (countBonusWin == 0) ? 0 : (double)countBonusWin / (double)countBonusWin;
        double totalWinOfWinTimes = (totalWinTimes == 0) ? 0 : (double)totalSpin / (double)totalWinTimes;

        double averageWinTotalGame = (totalWinTimes == 0) ? 0 : ((double)finalWin / (double)totalWinTimes);
        double averageWinBaseGame = (countBaseWin == 0) ? 0 : (double)finalBaseWin / (double)countBaseWin;
        double averageWinFreeGame = (countFreeWin == 0) ? 0 : (double)finalFreeWin / (double)countFreeWin;
        double averageWinBonusGame = (countBonusWin == 0) ? 0 : (double)finalBonusWin / (double)countBonusWin;

        int countEarnTimes = returnEarnTimes(earnList);
        double totalEarn = returnTotalEarn(earnList) ;


        System.out.println("============== 中獎率 ==============");
        System.out.println("總中獎率: " + nf.format(totalOddsOfTotalSpin));
        System.out.println("--------- 總體中獎率 ---------");
        System.out.println("主遊戲　: " + nf.format(baseOddsOfTotalSpin)); 		//主遊戲中獎次數 / totalSpin
        System.out.println("免費遊戲: " + nf.format(freeOddsOfTotalSpin)); 		//免費遊戲中獎次 / totalSpin
        System.out.println("小遊戲　: " + nf.format(bonusOddsOfTotalSpin));		//小遊戲中獎次數 / totalSpin
        System.out.println("--------- 個別中獎率 ---------");
        System.out.println("主遊戲　: " + nf.format(baseOddsOfBaseSpin)); 		//主遊戲中獎次數 / testTimes
        System.out.println("免費遊戲: " + nf.format(baseOddsOfFreeSpin)); 		//免費遊戲中獎次 / countFreeSpin
        System.out.println("小遊戲　: " + nf.format(baseOddsOfBonusSpin));		//小遊戲中獎次數 / countBonusWin

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
        System.out.println("免費遊戲贏分次數: " + countFreeWin);
        System.out.println("免費遊戲贏分次數平均局數: " + nf.format((double)countFreeSpin / (double)countFreeWin));
        System.out.println("免費遊戲贏分次數平均贏分: " + nf.format(averageWinFreeGame));
        System.out.println("免費遊戲贏分次數平均贏分倍數: " + nf.format(averageWinFreeGame / (double)totalBet));
        System.out.println("----------- 小遊戲 ------------");
        System.out.println("小遊戲贏分次數: " + countBonusWin);
        System.out.println("小遊戲贏分次數平均局數: " + nf.format((double)countBonusWin / 1));
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

    public static void showMaxWin(long totalBet, long maxTotalWin, long maxSpinWin, int[][] maxTotalWinGrid, int[][] maxSpinWinGrid){

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

    private static void printGrid(int[][] maxSpinWinGrid) {
        for(int i = 0; i < maxSpinWinGrid[0].length; i++){
            for(int j = 0; j < maxSpinWinGrid.length; j++){
                String result = (maxSpinWinGrid[j][i] < 10) ? "  "+maxSpinWinGrid[j][i] : " "+maxSpinWinGrid[j][i] ;
                System.out.print(result);
            }
            System.out.println();
        }
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
}
