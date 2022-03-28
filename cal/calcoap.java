import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Main {
  static int msg_no = 100; // sent how mant times
  static int msg_size = 100;
  static String filepath = "D:\\Users\\USER\\Desktop\\cal\\";
  static String filename = "coap_100_0_1";

  public static void main(String[] args) throws IOException {
    try {
      InputStreamReader isr = new InputStreamReader(new FileInputStream(filepath + filename));
      BufferedReader reader = new BufferedReader(isr);
      String line = null;
      line = reader.readLine();
      long tmp_token = 0;
      int msg_count = -1;
      long[] token_list = new long[msg_no + 1];
      int[] length_list = new int[msg_no + 1];
      int[] delay_list = new int[msg_no + 1];
      boolean[] suc_list = new boolean[msg_no + 1];
      boolean[] succ_list = new boolean[msg_no + 1];
      String[] token_listStr = new String[msg_no + 1];
      int block_no = 0;
      double efficiency = 0;
      for (int i = 0; i < msg_no; i++) {
        succ_list[i] = false;
      }

      while ((line = reader.readLine()) != null) {
        // System.out.println(line);
        // System.out.println(line.replace("\"CoAP\",", ""));
        // String item[] = line.replace("\"CoAP\",", "").split(",");
        String item[] = line.replace("\"CoAP\",", "").split(",");
        String data1 = item[0].trim(); // packet No.
        String data2 = item[1].trim(); // Time
        String data3 = item[2].trim(); // src ip
        String data4 = item[3].trim(); // dst ip
        String data5 = item[4].trim(); // length ->useful
        String data6 = item[5].trim(); // msg type
        String data7 = item[6].trim(); // mid
        String data8 = item[7].trim(); // status ->useful
        String data9 = item[8].trim(); // Token ->useful
        String data10 = item[9].trim(); // url or block number

        // System.out.println(data9);

        // System.out.print(data1+"\n"+ data2+"\n"+ data3+"\n");
        // System.out.print(data4+"\n"+ data5+"\n"+ data6+"\n");
        // System.out.print(data7+"\n"+ data8+"\n"+ data9+"\n"+ data10+"\n\n");
        data6 = data6.substring(1, data6.length());
        // System.out.println(data6);
        data9 = data9.replaceAll("\\s", "");
        // System.out.println(data9);
        data9 = data9.substring(4, data9.length());
        // System.out.println(data9);
        // System.out.println(data9);
        boolean new_message = true;
        int msg_order = 0;

        long token_l = Long.parseUnsignedLong(data9, 16);
        // System.out.print(token_l);
        int length = Integer.parseInt(data5.substring(1, data5.length() - 1));
        for (int i = 0; i <= msg_count; i++) {
          // System.out.println(token_listStr[i] + " " + data9);
          if (token_listStr[i].equals(data9)) {
            new_message = false;
            msg_order = i;
            // System.out.println(msg_order);
            // System.out.println("found, order:" + i);
          }
          // System.out.println("newmessage: " + new_message);
        }
        // new message
        if (new_message == true) {
          msg_count += 1;
          token_listStr[msg_count] = data9;
          token_list[msg_count] = token_l;
          msg_order = msg_count;
          // System.out.println("new message!!");
        }
        // Calculate Throughput
        length_list[msg_order] += Integer.parseInt(data5.substring(1, data5.length() - 1));
        if (data8.contains("Changed")) {
          suc_list[msg_order] = true;
        }
        // System.out.println("Message Order: " + msg_order);
      }
      reader.close();
      // check the last message

      block_no = 0;
      for (int i = 0; i < msg_no; i++) {
        // System.out.println("Message " + i + " length: " + length_list[i]);
        System.out.printf("Length[%d]: %d  Ack = %s Token = %s\n", i, length_list[i], suc_list[i],
            token_listStr[i]);
        if (suc_list[i] == true) {
          // System.out.println("true " + block_no);
          efficiency = efficiency + 1.0 / length_list[i];
          block_no++;
        }
      }
      efficiency = efficiency * msg_size / msg_no;
      System.out.println("successful transmission: " + block_no);
      System.out.println("Efficiency: " + efficiency); // print efficiency
      System.out.println("msgCount: " + msg_count);

      // Read success and delay
      try {
        InputStreamReader sucsr =
            new InputStreamReader(new FileInputStream(filepath + filename + "_na_success.txt"));
        BufferedReader sucreader = new BufferedReader(sucsr);
        InputStreamReader delaysr =
            new InputStreamReader(new FileInputStream(filepath + filename + "_na_delay.txt"));
        BufferedReader delayreader = new BufferedReader(delaysr);
        String delayline = null;
        String sucline = null;
        while ((sucline = sucreader.readLine()) != null) {
          String sucStr = sucline;
          delayline = delayreader.readLine();
          String delayStr = delayline;
          int sucInt = Integer.parseInt(sucStr);
          int delayInt = Integer.parseInt(delayStr);
          succ_list[sucInt - 1] = true;
          delay_list[sucInt - 1] = delayInt;
        }
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // write result to txt
      try {
        for (int temp = 0; temp < 100; temp++) {
          BufferedWriter bw =
              new BufferedWriter(new FileWriter(filepath + filename + "test.csv", true));
          // bw.write(Integer.toString(delay[temp]) + "," +
          // Integer.toString(length_list[temp]) + "," + success[temp] + "\n");
          if (delay_list[temp] != 0) {
            // length_list =>throughtput
            bw.write(Integer.toString(length_list[temp]) + "," + delay_list[temp] + ","
                + suc_list[temp] + "," + succ_list[temp] + "\n");
          } else
            bw.write(Integer.toString(length_list[temp]) + ","
                + ""
                + "," + suc_list[temp] + "," + succ_list[temp] + "\n");
          bw.flush();
          bw.close();
          // System.out.println("\007");
        }
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
