import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
  static int msg_no = 100;
  static int packet_no = 3;
  static int msg_size = 1000;
  static int start_seq = 1;
  static int seq_unit = 3445; // 1829, 2549, 3445, 4325

  static String filepath = "D:\\Users\\USER\\Desktop\\cal\\";
  // static String filename = "websocket_1500_0_5";3
  static String filename_1 = "xmpp_1000_20_5";
  // static String filename_2 = "websocket_166_10_5";
  static String filename_2 = filename_1;

  static String publisher_ip = "192.168.101.100";
  public static void main(String[] args) throws IOException {
    try {
      // InputStreamReader isr = new InputStreamReader(new
      // FileInputStream("verify0619/coap/mqtt_1500_20_3"));
      InputStreamReader isr = new InputStreamReader(new FileInputStream(filepath + filename_1));
      BufferedReader reader = new BufferedReader(isr);
      // BufferedWriter bw = new BufferedWriter(new
      // FileWriter("D://file_output.csv"));
      String line = null;
      line = reader.readLine();
      // long tmp_token = 0;
      int msg_count = 0;
      // long []token_list = new long[msg_no + 1];
      int[] length_list = new int[msg_no + 1];
      int[] delay_list = new int[msg_no + 1];
      boolean[] suc_list = new boolean[msg_no + 1];
      boolean[] succ_list = new boolean[msg_no + 1];
      double[] time_list = new double[msg_no + 1];
      // boolean [] check_ack = new boolean[packet_no + 1];
      boolean pshack = false;
      boolean from_publisher = false;

      // int block_no = 0;
      double efficiency = 0;
      int cur_len = 0;
      long cur_seq = 0;
      double time_temp = 0.0;
      double time_last = 0;
      int seq_offset = start_seq;
      int cal_count = 0;
      long seq = 0;
      long ack = 0;
      int len = 0;
      int msg_contains = 0;
      long cur_ack = 0;
      long back_throughput = 0;
      while ((line = reader.readLine()) != null) {
        String item[] = line.replace("\"TCP\",", "").replace("\"XMPP/XML\",", "").split("\",");
        // System.out.println(item[0]);
        String data1 = item[0].trim(); // packet No.
        String data2 = item[1].trim(); // Time
        String data3 = item[2].trim().substring(1); // src ip
        String data4 = item[3].trim().substring(1); // dst ip
        String data5 = item[4].trim(); // length
        String data6 = item[5].trim(); // msg type
        from_publisher = data3.equalsIgnoreCase(publisher_ip);
        pshack = data6.contains("[PSH, ACK]");
        int size_cur =
            Integer.parseInt((String) data5.substring(1, data5.length())); // packet length
        // System.out.println("From NSCL: " + from_nscl);
        // System.out.println("PSH ACK: " + pshack);
        // System.out.println(data6);
        if (!data6.contains("UNKNOWN") && !data6.contains("SACK_PERM") && !data6.contains("XMPP")) {
          // System.out.println(data1);
          seq = Long.parseUnsignedLong((String) data6.substring(data6.lastIndexOf("Seq=") + 4,
                                           data6.lastIndexOf("Ack=") - 1),
                    10)
              - seq_offset;
          ack = Long.parseUnsignedLong((String) data6.substring(data6.lastIndexOf("Ack=") + 4,
                                           data6.lastIndexOf("Win=") - 1),
                    10)
              - seq_offset;
          len = Integer.parseInt((String) data6.substring(data6.lastIndexOf("Len=") + 4,
                                     data6.lastIndexOf("TSval=") - 1),
              10);

          // int lenth_cur =
          // Integer.parseInt((String)data6.substring(data6.lastIndexOf("Len=")
          // + 4, data6.lastIndexOf(" TSv") ), 10);
        } else {
          seq = ack - seq_offset;
          len = seq_unit;
        }
        double time_cur = Double.parseDouble(data2.replace("\"", ""));
        if (time_temp == 0.0)
          time_temp = time_cur;
        // System.out.println(time_cur);

        // Packet from publisher
        if (from_publisher == true) {
          if (size_cur > 155) {
            int start_no = ((int) (seq) / seq_unit);
            int end_no = ((int) (seq + len - 1) / seq_unit);
            // System.out.println("Start number = " + start_no + "end_number = " + end_no);
            if (start_no == end_no) {
              length_list[start_no] += size_cur;
              // System.out.printf("%s: %d %d", data1, start_no, len);
            } else {
              long sta_seq = 0;
              long end_seq = 0;
              for (int i = start_no; i <= end_no; i++) {
                if ((i * seq_unit) < seq) {
                  sta_seq = seq;
                } else {
                  sta_seq = i * seq_unit;
                }
                if (((i + 1) * seq_unit) < (seq + len)) {
                  end_seq = (i + 1) * seq_unit;
                } else {
                  end_seq = seq + len;
                }
                length_list[i] += (int) size_cur * (end_seq - sta_seq) / len;
                // System.out.printf(" %d: %d ",i, (int)size_cur *  (end_seq -
                // sta_seq) / len );
              }
            }
          } else {
            back_throughput += size_cur;
          }

        } else {
          if (size_cur > 100) {
            // Calculate size
            length_list[(int) ((ack - 2) / seq_unit)] += size_cur;
            if ((int) ((ack - 2) / seq_unit) == 0) {
              // System.out.println("0ack, " + length_list[(int)((ack -
              // 2)/seq_unit)]);
            }
            suc_list[(int) ((ack - 2) / seq_unit)] = true;
            /*
            if(data6.contains("SLE")) {
                    int SLE =
            Integer.parseInt((String)data6.substring(data6.lastIndexOf("SLE=")+4,
            data6.lastIndexOf("SRE=") - 1));
                    int SRE =
            Integer.parseInt((String)data6.substring(data6.lastIndexOf("SRE=")+4,
            data6.length() - 1));
                    //System.out.printf("\nSLE: %d, SRE: %d\n", SLE, SRE);
                    SLE = (SLE - 2)/seq_unit;
                    //for(int i = (int)(cur_ack - 2)/seq_unit; i < SLE; i++) {
                    for(int i = 0; i < SLE; i++) {
                            suc_list[i] = true;
                    }

            }*/
          } else {
            back_throughput += size_cur;
          }
          cur_ack = ack;
        }
        time_last = time_cur;
        // System.out.printf("msg_count: %d, calculated_count: %d\n", msg_count,
        // cal_count);
      }
      // time_list[msg_count] = time_last - time_list[msg_count];
      // length_list[msg_count++] = cur_len;

      int suc_count = 0;
      for (int i = 0; i < 100; i++) {
        System.out.printf("Length[%d]: %d  Ack = %s\n", i, length_list[i], suc_list[i]);
        // System.out.printf("Round trip delay: %fs\n\n", time_list[i]);
        if (suc_list[i]) {
          efficiency += 1.0 / length_list[i];
          suc_count++;
        }
      }
      // Read success and delay
      try {
        InputStreamReader sucsr =
            new InputStreamReader(new FileInputStream(filepath + filename_2 + "_na_success.txt"));
        BufferedReader sucreader = new BufferedReader(sucsr);
        // BufferedWriter bw = new BufferedWriter(new
        // FileWriter("D://file_output.csv"));
        InputStreamReader delaysr =
            new InputStreamReader(new FileInputStream(filepath + filename_2 + "_na_delay.txt"));
        BufferedReader delayreader = new BufferedReader(delaysr);
        String delayline = null;
        String sucline = null;
        while ((sucline = sucreader.readLine()) != null) {
          String sucStr = sucline;
          delayline = delayreader.readLine();
          String delayStr = delayline;
          int sucInt = Integer.parseInt(sucStr);
          int delayInt = Integer.parseInt(delayStr);
          if (sucInt == 0)
            sucInt = 100;
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
              new BufferedWriter(new FileWriter(filepath + filename_1 + "_event_output.csv", true));
          //							bw.write(Integer.toString(delay[temp])
          //+
          //"," + Integer.toString(length_list[temp]) + "," + success[temp] +
          //"\n");
          if (delay_list[temp] != 0) {
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
      // //write background throughput
      // try {
      // BufferedWriter bw = new BufferedWriter(new
      // FileWriter("Event/Output/xmpp/background_query_output.csv", true));
      // //
      // bw.write(Integer.toString(delay[temp]) + "," +
      // Integer.toString(length_list[temp]) + "," + success[temp] + "\n");
      // bw.write(filename_1 + "," + Long.toString(back_throughput) + "\n");
      // bw.flush();
      // bw.close();
      // //System.out.println("\007");
      //
      // }catch (FileNotFoundException e) {
      // // TODO Auto-generated catch block
      // e.printStackTrace();
      // }

      efficiency = efficiency * msg_size / msg_no;
      System.out.println("successful transmission: " + suc_count);
      System.out.println("Average Efficiency: " + efficiency);
      System.out.println("Background throughput: " + back_throughput);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
