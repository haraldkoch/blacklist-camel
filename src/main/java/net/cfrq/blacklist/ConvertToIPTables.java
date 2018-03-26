package net.cfrq.blacklist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sun.net.util.IPAddressUtil;

@SuppressWarnings("unused")
public class ConvertToIPTables {
    
    public static List<String> process(List<Map<String, Object>> blacklist) {
        ArrayList<String> firewall = new ArrayList<>(blacklist.size() + 18);

        firewall.add("/sbin/iptables -N BLACKLIST2");
        firewall.add("/sbin/iptables -t filter -A BLACKLIST2 -j RETURN -p udp --dport 53");
        firewall.add("/sbin/iptables -t filter -A BLACKLIST2 -j RETURN -p tcp --dport 53");

        firewall.add("/sbin/ip6tables -N BLACKLIST2");
        firewall.add("/sbin/ip6tables -t filter -A BLACKLIST2 -j RETURN -p udp --dport 53");
        firewall.add("/sbin/ip6tables -t filter -A BLACKLIST2 -j RETURN -p tcp --dport 53");

        for (Map<String, Object> stringObjectMap : blacklist) {
            String rule;
            String ip = (String)stringObjectMap.get("ip");
            if (isIPv6(ip)) {
                rule = "/sbin/ip6tables -t filter -A BLACKLIST2 -j DROP -s " + ip;
            }
            else {
                rule = "/sbin/iptables -t filter -A BLACKLIST2 -j DROP -s " + ip;
            }
            
            if (stringObjectMap.get("type").equals("http")) {
                rule += " -p tcp -m multiport --dport http,https";
            }
            else if (stringObjectMap.get("type").equals("smtp")) {
                rule += " -p tcp -m multiport --dport pop3,pop3s,imap,imaps,smtp,smtps";
            }

            firewall.add(rule);
        }

        firewall.add("num=`/sbin/iptables -n -L INPUT|nl -|grep BLACKLIST|awk '{print $1}'`");
        firewall.add("num=`expr $num - 2`");
        firewall.add("/sbin/iptables -R INPUT $num -j BLACKLIST2");
        firewall.add("/sbin/iptables -F BLACKLIST");
        firewall.add("/sbin/iptables -X BLACKLIST");
        firewall.add("/sbin/iptables -E BLACKLIST2 BLACKLIST");

        firewall.add("num=`/sbin/ip6tables -n -L INPUT|nl -|grep BLACKLIST|awk '{print $1}'`");
        firewall.add("num=`expr $num - 2`");
        firewall.add("/sbin/ip6tables -R INPUT $num -j BLACKLIST2");
        firewall.add("/sbin/ip6tables -F BLACKLIST");
        firewall.add("/sbin/ip6tables -X BLACKLIST");
        firewall.add("/sbin/ip6tables -E BLACKLIST2 BLACKLIST");
        
        return firewall;
    }

    private static boolean isIPv6(String ip) {
        String[] split = ip.split("/");
        return IPAddressUtil.isIPv6LiteralAddress(split[0]);
    }
}
