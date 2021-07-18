package com.wyc.androiddemo.utils;

import java.util.Arrays;

/**
 * 作者： wyc
 * <p>
 * 创建时间： 2021/1/12 16:58
 * <p>
 * 文件名字： com.wyc.androiddemo.media
 * <p>
 * 类的介绍：
 */
public class PositionUtils {

    private final static double pi = 3.14159265358979324;
    private final static double a = 6378245.0;
    private final static double ee = 0.00669342162296594323;
    private final static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

    /**
     * 火星坐标系 (GCJ-02) to WGS84
     *
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    public static double[] gcj2wgs(double lat, double lon) {
        double[] gps = transform(lat, lon);
        double longitude = lon * 2 - gps[1];
        double latitude = lat * 2 - gps[0];
        return new double[]{latitude, longitude};
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标
     *
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    public static double[] gcj2bd(double lat, double lon) {
        double z = Math.sqrt(lon * lon + lat * lat) + 0.00002 * Math.sin(lat * x_pi);
        double theta = Math.atan2(lat, lon) + 0.000003 * Math.cos(lon * x_pi);
        double bd_lon = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new double[]{bd_lat, bd_lon};
    }

    /**
     * 百度坐标系 (BD-09) to 火星坐标系 (GCJ-02)
     *
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    public static double[] bd2gcj(double lat, double lon) {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return new double[]{gg_lat, gg_lon};
    }

    /**
     * WGS84 to 火星坐标系 (GCJ-02)
     *
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    public static double[] wgs2gcj(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new double[]{0, 0};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    /**
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    private static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        return lat < 0.8293 || lat > 55.8271;
    }

    /**
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    public static double[] transform(double lat, double lon) {
        if (outOfChina(lat, lon)) {
            return new double[]{lat, lon};
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new double[]{mgLat, mgLon};
    }

    /**
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    private static double transformLat(double lat, double lon) {
        double ret = -100.0 + 2.0 * lat + 3.0 * lon + 0.2 * lon * lon + 0.1 * lat * lon
                + 0.2 * Math.sqrt(Math.abs(lat));
        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lon * pi) + 40.0 * Math.sin(lon / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lon / 12.0 * pi) + 320 * Math.sin(lon * pi / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * @param lat 维度
     * @param lon 经度
     * @return
     */
    private static double transformLon(double lat, double lon) {
        double ret = 300.0 + lat + 2.0 * lon + 0.1 * lat * lat + 0.1 * lat * lon + 0.1 * Math.sqrt(Math.abs(lat));
        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * pi) + 40.0 * Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lat / 12.0 * pi) + 300.0 * Math.sin(lat / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

    public static void main(String[] args) {
        // 北斗芯片获取的经纬度为WGS84地理坐标 31.426896,119.496145
        double[] gps = new double[]{31.426896, 119.496145};
        System.out.println("gps :" + Arrays.toString(gps));
        double[] gcj = wgs2gcj(gps[0], gps[1]);
        System.out.println(Arrays.toString(gcj));
        gcj = gcj2bd(gcj[0], gcj[1]);
        System.out.println(Arrays.toString(gcj));
    }
}
