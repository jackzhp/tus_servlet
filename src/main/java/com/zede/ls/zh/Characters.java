/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zede.ls.zh;

import java.util.HashSet;

/**
 *
 * @author yogi
 */
public class Characters {

    static String a = "这些年 一个人 风也过 雨也走\n"
            + "有过泪 有过错 还记得坚持什么\n"
            + "\n"
            + "真爱过 才会懂 会寂寞 会回首\n"
            + "终有梦 终有你 在心中\n"
            + "\n"
            + "朋友 一生一起走 那些日子 不再有\n"
            + "一句话 一辈子 一生情 一杯酒\n"
            + "\n"
            + "朋友 不曾孤单过 一声朋友 你会懂\n"
            + "还有伤 还有痛 还要走 还有我\n"
            + "\n"
            + "这些年 一个人 风也过 雨也走\n"
            + "有过泪 有过错 还记得坚持什么\n"
            + "\n"
            + "真爱过 才会懂 会寂寞 会回首\n"
            + "终有梦 终有你 在心中\n"
            + "\n"
            + "朋友 一生一起走 那些日子 不再有\n"
            + "一句话 一辈子 一生情 一杯酒\n"
            + "\n"
            + "朋友 不曾孤单过 一声朋友 你会懂\n"
            + "还有伤 还有痛 还要走 还有我\n"
            + "\n"
            + "朋友 一生一起走 那些日子 不再有\n"
            + "一句话 一辈子 一生情 一杯酒\n"
            + "\n"
            + "朋友 不曾孤单过 一声朋友 你会懂\n"
            + "还有伤 还有痛 还要走 还有我\n"
            + "\n"
            + "朋友 一生一起走 那些日子 不再有\n"
            + "一句话 一辈子 一生情 一杯酒\n"
            + "\n"
            + "朋友 不曾孤单过 一声朋友 你会懂\n"
            + "还有伤 还有痛 还要走 还有我\n"
            + "\n"
            + "一句话 一辈子 一生情 一杯酒";

    static String s2 = "甜蜜蜜\n"
            + "你笑得甜蜜蜜\n"
            + "好像花儿开在春风里\n"
            + "开在春风里\n"
            + "在哪里\n"
            + "在哪里见过你\n"
            + "你的笑容这样熟悉\n"
            + "我一时想不起\n"
            + "啊在梦里\n"
            + "梦里梦里见过你\n"
            + "甜蜜笑得多甜蜜\n"
            + "是你是你\n"
            + "梦见的就是你\n"
            + "在哪里\n"
            + "在哪里见过你\n"
            + "你的笑容这样熟悉\n"
            + "我一时想不起\n"
            + "啊在梦里\n"
            + "在哪里\n"
            + "在哪里见过你\n"
            + "你的笑容这样熟悉\n"
            + "我一时想不起…";

    static String s3 = "这些年 一个人 风也过 雨也走\n"
            + "有过泪 有过错 还记得坚持什么\n"
            + "\n"
            + "真爱过 才会懂 会寂寞 会回首\n"
            + "终有梦 终有你 在心中\n"
            + "\n"
            + "朋友 一生一起走 那些日子 不再有\n"
            + "一句话 一辈子 一生情 一杯酒\n"
            + "\n"
            + "朋友 不曾孤单过 一声朋友 你会懂\n"
            + "还有伤 还有痛 还要走 还有我\n"
            + "";

    public static void main(String[] args) {
        String s = s3;////s3; //s2; //
//        a; 一持要辈有朋不再我首得错会些真生那伤梦在个中记爱人什懂寂心情过么终友才风子酒单还这坚痛话寞回也你孤日句雨泪杯走声年起曾
//       s3  一持要辈有朋不再我首得错会些真生那伤梦在个中记爱人什懂寂心情过么终友才风子酒单还这坚痛话寞回也你孤日句雨泪杯走声年起曾

        char[] ac = s.toCharArray();
        HashSet<Character> set = new HashSet<>();
        for (char c : ac) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            set.add(c);
        }
        System.out.println("#:" + set.size()); //61, 59
        for (Character c : set) {
            System.out.print(c);
        }

    }

}



/*
2 versions:
#1 with meaning interleaving
#2 compact(only Chinese)

这:this 些: plural 年:year  一:one 个: 人:man 风:wing 也:also 过:pass over 雨:rain 也:also 走:walk    
有:have/has 过 泪:tear 有过错:wrong 还:still 记:remember 得 坚持:mindfully to keep goinng 什么:what

真:really 爱:love 过 才:only 会:able 懂:understand 会寂寞:lonely 会回:back 首:head
终:eventually 有梦:dream 终有你:you 在:at/in 心:heart 中:inside

朋友:Friends 一生:life 一起:together 走 那些:those 日子:days 不再:no more 有
一句话:one sentence 一辈子:one life 一生情: one life's love  一杯酒: one cup of drink

朋友 不:not 曾:once 孤单:lonly 过 一声:one sound 朋友 你会懂
还有伤:hurts 还有痛:pains 还要走 还有我:me






这些年 一个人 风也过 雨也走
有过泪 有过错 还记得坚持什么

真爱过 才会懂 会寂寞 会回首
终有梦 终有你 在心中

朋友 一生一起走 那些日子 不再有
一句话 一辈子 一生情 一杯酒

朋友 不曾孤单过 一声朋友 你会懂
还有伤 还有痛 还要走 还有我

朋友 一生一起走 那些日子 不再有
一句话 一辈子 一生情 一杯酒

朋友 不曾孤单过 一声朋友 你会懂
还有伤 还有痛 还要走 还有我

朋友 一生一起走 那些日子 不再有
一句话 一辈子 一生情 一杯酒

朋友 不曾孤单过 一声朋友 你会懂
还有伤 还有痛 还要走 还有我

一句话 一辈子 一生情 一杯酒
*/
