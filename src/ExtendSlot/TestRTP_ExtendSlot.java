package ExtendSlot;



import com.gameExtendSlotLogic.sfs2x.ExtendSlotLogic;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

public class TestRTP_ExtendSlot {

    static int gameID = 328;
    static int testTimes = 1000;
    static int lineBet = 1;
    static boolean isRateStatic = true;
    static boolean isAlert = true;
    static long bank = 10000;

    static ExtendSlotLogic slot = new ExtendSlotLogic(gameID);

    static int totalSpin;


    @SuppressWarnings("unchecked")
    public static void main(String[] args){
        double rate = 104;
        double prob = 104;

        long totalBet = slot.getTotalBet(lineBet);
        long totalWin;
        long totalBaseWin;              //單轉主遊戲
        long totalFreeWin;              //單轉免費遊戲
//        long totalBonusWin;             //單轉Bonus

        double earn;                    //單次淨利
        double[] earnList = new double[testTimes];
        double earnStandDev;    //淨利標準差

        long finalBet = 0;
        long finalWin = 0;
        long finalBaseWin = 0;
        long finalFreeWin = 0;


        int countTotalWin = 0;
        int countBaseWin = 0;
        int countFreeWin = 0;
        int countFreeSpinWin = 0; //計算單轉免費遊戲中獎次數


        long finalSecretWin = 0;    //所有局數出神秘圖標總贏分
        int countIsSecret = 0;      //所有局數出神秘圖標的總次數
        int countTotalSecrets = 0;  //所有局數總出現的神祕圖標(結束後要除以countIsSecret 作為平均每次出現神秘圖標平均有幾個)


        int countWin10To30 = 0;
        int countWin30To50 = 0;
        int countWin50Up = 0;

        long maxTotalWin = 0;	//投注一次最大獲利金額
        long maxSpinWin = 0;	//單轉一次最大獲利金額
        long maxFreeSpinWin = 0;//免費遊戲單轉最大獲利金額

        int[][] maxTotalWinGrid = null;//單局最高贏分圖示表
        int[][] maxSpinWinGrid = null;	//單轉最高贏分圖示表
        int[][] maxFreeSpinWinGrid = null;//免費遊戲單轉最高贏分圖表

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

            //初始化stickyChange
            int[][] stickyChange = new int[slot.getMaxReel()][slot.getMaxGrid()];
            for(int r = 0; r < slot.getMaxReel(); r++){
                for(int g = 0; g < slot.getMaxGrid(); g++){
                    stickyChange[r][g] = 0;
                    if(r == 2 && g == 1) stickyChange[r][g] = 1;
                }
            }

            //主遊戲-->
            HashMap<String,Object> respBase = slot.getBaseResult(lineBet, rate, prob);

            totalBaseWin = (long)respBase.get("TotalWin");
            finalBaseWin += totalBaseWin;
            finalWin += totalBaseWin;

            //計算主遊戲中獎次數  && 更新最大獎
            if(totalBaseWin > 0) countBaseWin++;
            if(totalBaseWin > maxSpinWin){
                maxSpinWin = totalBaseWin;
                maxSpinWinGrid = (int[][])respBase.get("Grid");
            }

            //計算神秘圖標 次數 個數 全部加總最後平均
            boolean isSecret = (boolean)respBase.get("IsSecret");
            if(isSecret){
                countIsSecret++;
                finalSecretWin += (long)respBase.get("TotalWin");
                int[][] secretGrid = (int[][])respBase.get("SecretChange");
                for(int sr = 0; sr < slot.getMaxReel(); sr++){
                    for(int sg = 0; sg < slot.getMaxGrid(); sg++){
                        if(secretGrid[sr][sg] == 1){
                            countTotalSecrets++;
                        }
                    }
                }

            }

            //主遊戲後 rate
            rate = (isRateStatic) ? rate : (( (double)finalWin / (double)finalBet ) * 100) ;

            //<--主遊戲結束
            //免費遊戲-->
            if((int)respBase.get("FreeSpinCount") > 0){
                int countDownFree = (int)respBase.get("FreeSpinCount");
                countBaseToFreeType = classifyFreeGame(countBaseToFreeType, countDownFree, false);		//分類 主中免 次數列表

                //免費遊戲次數迴圈
                for(int j = 0; j < countDownFree; j++){
                    HashMap<String, Object> respFree = slot.getFreeResult(lineBet, rate, prob, stickyChange, isAlert);
                    //更新stickyChange
                    stickyChange = (int[][])respFree.get("StickyChange");

                    //免費遊戲 中 免費遊戲-->
                    if((int)respFree.get("FreeSpinCount") > 0){
                        countDownFree += (int)respFree.get("FreeSpinCount");
                        countFreeToFreeType = classifyFreeGame(countFreeToFreeType, (int)respFree.get("FreeSpinCount"), true); //分類 免中免 次數列表
                    }
                    //紀錄單轉最大獎項
                    if((long)respFree.get("TotalWin") > maxSpinWin){
                        maxSpinWin = (long)respFree.get("TotalWin");
                        maxSpinWinGrid = (int[][])respFree.get("Grid");
                    }
                    //只記錄免費遊戲最大獎項
                    if((long)respFree.get("TotalWin") > maxFreeSpinWin){
                        maxFreeSpinWin = (long)respFree.get("TotalWin");
                        maxFreeSpinWinGrid = (int[][])respFree.get("Grid");
                    }

                    if((long)respFree.get("TotalWin") > 0) countFreeSpinWin++; //計算單轉有獲利..用於計算免費轉數的中獎率

                    totalFreeWin += (long)respFree.get("TotalWin");
                    finalWin += (long)respFree.get("TotalWin");
                    //免費遊戲後 rate
                    rate = (isRateStatic) ? rate :  (( (double)finalWin / (double)finalBet) * 100);
                }
                if(totalFreeWin > 0) countFreeWin++;
                finalFreeWin += totalFreeWin;
            }//<--免費遊戲結束


            //本局結束 計算本局遊戲數據-->
            totalWin = totalBaseWin + totalFreeWin ;
            //更新bank
            bank += (totalBet * 96) / 100 - totalWin;


            if(totalWin > 0)
            {
                countTotalWin++;								//紀錄中獎次數
                if(totalWin > maxTotalWin){
                    maxTotalWin = totalWin;
                    maxTotalWinGrid = (int[][])respBase.get("Grid");
                }
                if( (totalWin / totalBet) > 50) countWin50Up++;			//超級巨獎次數
                else if( (totalWin / totalBet) > 30) countWin30To50++;	//巨獎次數
                else if( (totalWin / totalBet) > 10) countWin10To30++;	//大獎次數
            }

            earn = totalWin - totalBet;
            earnList[i] = earn;
            //<--計算本局遊戲數據
        }
        //<--迴圈結束

        //迴圈結束處理數據
        rate = ((double)finalWin / (double)finalBet) *100;
        countFreeSpin = returnFreeGameTimes(countBaseToFreeType, countFreeToFreeType);
        totalSpin = testTimes + countFreeSpin;


        //迴圈結束印出所有數據-->
        showGameInfo(gameID, testTimes);
        showGameRTP(finalBet, finalWin, finalBaseWin, finalFreeWin, rate);
        showBank();
        showGameCount(totalSpin, testTimes, countFreeSpin, countBaseToFreeType, countFreeToFreeType,finalFreeWin);
        showOdds(totalSpin, testTimes, countFreeSpin, countBaseWin, countFreeWin, countFreeSpinWin, finalWin, finalBaseWin, finalFreeWin, totalBet, earnList);
        showSecretOdds(finalSecretWin, countIsSecret, countTotalSecrets);
        showAward(totalSpin, countWin10To30, countWin30To50, countWin50Up);
        showMaxWin(totalBet, maxTotalWin, maxSpinWin, maxFreeSpinWin, maxTotalWinGrid, maxSpinWinGrid, maxFreeSpinWinGrid);
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
        System.out.println("測試遊戲： " + gameID);
        System.out.println("測試次數： " + testTimes);
    }
    public static void showGameRTP(long finalBet, long finalWin, long finalBaseWin, long finalFreeWin, double rate){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        double baseRTP = ((double)finalBaseWin / (double)finalBet) * 100;
        double freeRTP = ((double)finalFreeWin / (double)finalBet) * 100;
//        double bonusRTP = ((double)finalBonusWin / (double)finalBet) * 100;

        System.out.println("============== 總獲利 =============="); //28個=
        System.out.println("總押分　　　: " + finalBet);
        System.out.println("總贏分　　　: " + finalWin);
        System.out.println("主遊戲贏分　: " + finalBaseWin);
        System.out.println("免費遊戲贏分: " + finalFreeWin);
//        System.out.println("小遊戲贏分　: " + finalBonusWin);

        System.out.println("=============== RTP ==============="); //30個=
        System.out.println("整體　　: " + nf.format(rate));
        System.out.println("主遊戲　: " + nf.format(baseRTP));
        System.out.println("免費遊戲: " + nf.format(freeRTP));
//        System.out.println("小遊戲　: " + nf.format(bonusRTP));
    }
    public static void showBank(){
        System.out.println("=============== BANK ==============="); //30個=
        System.out.println("Bank　 : " + bank);
    }
    public static void showGameCount(int totalSpin, int testTimes, int countFreeSpin, ArrayList<Integer> countBaseToFreeType, ArrayList<Integer> countFreeToFreeType, long finalFreeWin){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        double averageBaseToFree 	= (double)testTimes / (double)countBaseToFreeType.get(0);
        double averageFreeToFree 	= (double)countBaseToFreeType.get(0) / (double)countFreeToFreeType.get(0);
        double averageFreeWin 		= (double)finalFreeWin / (double)countBaseToFreeType.get(0);
        double averageFreeWinInSpin = (double)finalFreeWin / (double)countFreeSpin;
        double averageFreeSpin 		= (double)countFreeSpin/ (double)countBaseToFreeType.get(0);


        System.out.println("========== 局數及中獎次數 ===========");
        System.out.println("總局數: " + totalSpin);
        System.out.printf("主遊戲: %d; 免費遊戲: %d;\n", testTimes, countFreeSpin);
        System.out.println("中免費遊戲總次數: " + (countBaseToFreeType.get(0) + countFreeToFreeType.get(0)));
        System.out.printf("主中免次數: %d = (%d)+(%d)+(%d) \n", countBaseToFreeType.get(0), countBaseToFreeType.get(1), countBaseToFreeType.get(2), countBaseToFreeType.get(3));
        System.out.printf("免中免次數: %d = (%d)+(%d)+(%d) \n", countFreeToFreeType.get(0), countFreeToFreeType.get(1), countFreeToFreeType.get(2), countFreeToFreeType.get(3));
        System.out.println("平均主中免局數　: " + nf.format(averageBaseToFree));
        System.out.println("平均免中免局數　: " + nf.format(averageFreeToFree));
        System.out.println("免費遊戲平均贏分: " + nf.format(averageFreeWin));
        System.out.println("平均每轉免費贏分: " + nf.format(averageFreeWinInSpin));
        System.out.println("平均主中免能轉　: " + nf.format(averageFreeSpin));

    }
    public static void showOdds(int totalSpin, int testTimes, int countFreeSpin, int countBaseWin, int countFreeWin, int countFreeSpinWin,
                                long finalWin, long finalBaseWin, long finalFreeWin, long totalBet, double[] earnList){
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);

        int totalWinTimes = countBaseWin + countFreeSpinWin;
        double totalOddsOfTotalSpin = (double)totalWinTimes / (double)totalSpin;	//總中獎率

        double baseOddsOfTotalSpin = (double)countBaseWin / (double)totalSpin;		//總體-主遊戲中獎率
        double freeOddsOfTotalSpin = (double)countFreeSpinWin / (double)totalSpin;	//總體-免費遊戲中獎率
//        double bonusOddsOfTotalSpin = (double)countBonusWin / (double)totalSpin;	//總體-小遊戲中獎率

        double baseOddsOfBaseSpin = (double)countBaseWin / (double)testTimes;
        double baseOddsOfFreeSpin = (countFreeSpin == 0) ? 0 : (double)countFreeSpinWin / (double)countFreeSpin;
//        double baseOddsOfBonusSpin = (countBonusWin == 0) ? 0 : (double)countBonusWin / (double)countBonusWin;
        double totalWinOfWinTimes = (totalWinTimes == 0) ? 0 : (double)totalSpin / (double)totalWinTimes;

        double averageWinTotalGame = (totalWinTimes == 0) ? 0 : ((double)finalWin / (double)totalWinTimes);
        double averageWinBaseGame = (countBaseWin == 0) ? 0 : (double)finalBaseWin / (double)countBaseWin;
        double averageWinFreeGame = (countFreeWin == 0) ? 0 : (double)finalFreeWin / (double)countFreeWin;
//        double averageWinBonusGame = (countBonusWin == 0) ? 0 : (double)finalBonusWin / (double)countBonusWin;

        int countEarnTimes = returnEarnTimes(earnList);
        double totalEarn = returnTotalEarn(earnList) ;


        System.out.println("============== 中獎率 ==============");
        System.out.println("總中獎率: " + nf.format(totalOddsOfTotalSpin));
        System.out.println("--------- 總體中獎率 ---------");
        System.out.println("主遊戲　: " + nf.format(baseOddsOfTotalSpin)); 		//主遊戲中獎次數 / totalSpin
        System.out.println("免費遊戲: " + nf.format(freeOddsOfTotalSpin)); 		//免費遊戲中獎次 / totalSpin
//        System.out.println("小遊戲　: " + nf.format(bonusOddsOfTotalSpin));		//小遊戲中獎次數 / totalSpin
        System.out.println("--------- 個別中獎率 ---------");
        System.out.println("主遊戲　: " + nf.format(baseOddsOfBaseSpin)); 		//主遊戲中獎次數 / testTimes
        System.out.println("免費遊戲: " + nf.format(baseOddsOfFreeSpin)); 		//免費遊戲中獎次 / countFreeSpin
//        System.out.println("小遊戲　: " + nf.format(baseOddsOfBonusSpin));		//小遊戲中獎次數 / countBonusWin

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
//        System.out.println("----------- 小遊戲 ------------");
//        System.out.println("小遊戲贏分次數: " + countBonusWin);
//        System.out.println("小遊戲贏分次數平均局數: " + nf.format((double)countBonusWin / 1));
//        System.out.println("小遊戲贏分次數平均贏分: " + nf.format(averageWinBonusGame));
//        System.out.println("小遊戲贏分次數平均贏分倍數: " + nf.format(averageWinBonusGame / (double)totalBet));

    }
    private static void showSecretOdds(long finalSecretWin, int countIsSecret, int countTotalSecrets) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);

        double secretOdds = (double) countIsSecret / (double)testTimes;  //神秘模式中獎率
        double averageSecrets = (double) countTotalSecrets / (double)countIsSecret; //平均出牌數
        long totalBet = slot.getTotalBet(lineBet);
        double averageSecretWin = (double) finalSecretWin / (double) countIsSecret; //平均贏分
        double averageSecretWinTimes = averageSecretWin / (double) totalBet;        //平均贏分倍數

        System.out.println("----------- 神秘模式 -----------");
        System.out.println("神秘圖標次數　　　　: " + countIsSecret);
        System.out.println("神秘圖標中獎率　　　: " + nf.format(secretOdds));
        System.out.println("神秘圖標平均出牌數　: " + nf.format(averageSecrets));
        System.out.println("神秘圖標平均贏分　　: " + nf.format(averageSecretWin));
        System.out.println("神秘圖標平均贏分倍數: " + nf.format(averageSecretWinTimes));



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
    public static void showMaxWin(long totalBet, long maxTotalWin, long maxSpinWin, long maxFreeSpinWin,
                                  int[][] maxTotalWinGrid, int[][] maxSpinWinGrid, int[][] maxFreeSpinWinGrid){

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
        System.out.println("免費遊戲單轉最高贏分: " + maxFreeSpinWin);
        System.out.println("單轉最高贏分倍數: " + ((double)maxFreeSpinWin / (double)totalBet));
        System.out.println("單轉最高組合如下");
        printGrid(maxFreeSpinWinGrid);
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


}
