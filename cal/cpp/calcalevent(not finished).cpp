#include <bits/stdc++.h>
using namespace std;

// for event
// if not sucess efficiency = 0
// 0 coap
// 1 mqtt
// 2 xmpp
// 3 ws
// {protocol,size,packet loss rate} => average throughput
// efficiency <=> event here is efficiency

int main() {
  // protocol,size,loss rate
  map<array<int, 3>, double> m;

  // coap
  m[{0, 100, 0}] = 283.992;
  m[{0, 500, 0}] = 683.752;
  m[{0, 1000, 0}] = 1183.992;
  m[{0, 1500, 0}] = 2066.76;

  m[{0, 100, 10}] = 340.624;
  m[{0, 500, 10}] = 856.192;
  m[{0, 1000, 10}] = 1433.61;
  m[{0, 1500, 10}] = 2537.524;

  m[{0, 100, 20}] = 422.704;
  m[{0, 500, 20}] = 1068.574;
  m[{0, 1000, 20}] = 1784.252;
  m[{0, 1500, 20}] = 3058.824;

  m[{0, 100, 30}] = 514.3333333;
  m[{0, 500, 30}] = 1352.71;
  m[{0, 1000, 30}] = 2271.22;
  m[{0, 1500, 30}] = 3848.254;

  // mqtt
  m[{1, 100, 0}] = 353.604;
  m[{1, 500, 0}] = 753.34;
  m[{1, 1000, 0}] = 1253.868;
  m[{1, 1500, 0}] = 1821.984;

  m[{1, 100, 10}] = 413.92;
  m[{1, 500, 10}] = 872.764;
  m[{1, 1000, 10}] = 1428.636;
  m[{1, 1500, 10}] = 2044.452;

  m[{1, 100, 20}] = 498.216;
  m[{1, 500, 20}] = 1003.728;
  m[{1, 1000, 20}] = 1772.892;
  m[{1, 1500, 20}] = 2271.204;

  m[{1, 100, 30}] = 569.864;
  m[{1, 500, 30}] = 1242.908;
  m[{1, 1000, 30}] = 2171.644;
  m[{1, 1500, 30}] = 4022 / ;

  // xmpp
  m[{2, 100, 0}] = 492.342;
  m[{2, 500, 0}] = 890.242;
  m[{2, 1000, 0}] = 1384.51;
  m[{2, 1500, 0}] = 1912.656;

  m[{2, 100, 10}] = 609.014;
  m[{2, 500, 10}] = 1084.934;
  m[{2, 1000, 10}] = 1761.67;
  m[{2, 1500, 10}] = 2248.01;

  m[{2, 100, 20}] = 746.22;
  m[{2, 500, 20}] = 1337.744;
  m[{2, 1000, 20}] = 1960.796;
  m[{2, 1500, 20}] = 2663.53;

  m[{2, 100, 30}] = 1002.618;
  m[{2, 500, 30}] = 1709.032;
  m[{2, 1000, 30}] = 2603.74;
  m[{2, 1500, 30}] = 2801.478;

  // ws
  m[{3, 100, 0}] = 306;
  m[{3, 500, 0}] = 706;
  m[{3, 1000, 0}] = 1206;
  m[{3, 1500, 0}] = 1722.704;

  m[{3, 100, 10}] = 369.96;
  m[{3, 500, 10}] = 901.42;
  m[{3, 1000, 10}] = 1470.432;
  m[{3, 1500, 10}] = 2016.668;

  m[{3, 100, 20}] = 463.092;
  m[{3, 500, 20}] = 1075.612;
  m[{3, 1000, 20}] = 1818.004;
  m[{3, 1500, 20}] = 2341.494;

  m[{3, 100, 30}] = 472.954;
  m[{3, 500, 30}] = 1250.876;
  m[{3, 1000, 30}] = 2096.568;
  m[{3, 1500, 30}] = 2639.49;

  // Settings
  string line;

  // Open file
  ifstream myFile;
  myFile.open("D:\\Users\\USER\\Desktop\\cal\\cpp\\aaa.txt");

  // Print file content
  while (getline(myFile, line)) {
    cout << line << endl;
  }

  // Close file
  myFile.close();

  return 0;
}

// std::ifstream ifs("D:\\Users\\USER\\Desktop\\cal\\aaa.txt", std::ios::in);