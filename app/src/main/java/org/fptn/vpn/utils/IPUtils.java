package org.fptn.vpn.utils;

import java.util.List;
import java.util.Optional;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

public class IPUtils {
    public static void exclude(IPAddress rootSubnet, List<IPAddress> subnetsToExclude, List<IPAddress> afterExclude) {
        Optional<IPAddress> any = subnetsToExclude.stream().filter(subnet -> subnet.equals(rootSubnet)).findAny();
        if (any.isPresent()) {
            // we reach minimum size target subnet
            //System.out.println("rootSubnet: " + rootSubnet + " == any: " + any.get());
            return;
        }

        int newNetmaskBits = rootSubnet.getNetworkPrefixLength() + 1;
        if (newNetmaskBits > 32) {
            //System.out.println("EXCEED NETMASK BITS COUNT");
            return;
        }

        IPAddress rootSubnetLower = rootSubnet.getLower();
        IPAddress subnetLeft = new IPAddressString(rootSubnetLower.toAddressString().getHostAddress() + "/" + newNetmaskBits).getAddress();
        //System.out.println("SubnetLeft: " + subnetLeft + " start from: " + subnetLeft.getLower() + " to: " + subnetLeft.getUpper());
        Optional<IPAddress> checkLeft = subnetsToExclude.stream().filter(subnetLeft::contains).findFirst();
        if (checkLeft.isPresent()) {
            exclude(subnetLeft, subnetsToExclude, afterExclude);
        } else {
            afterExclude.add(subnetLeft);
        }

        IPAddress[] subtract = rootSubnet.subtract(subnetLeft);
        //System.out.println("subtract: " + subtract);
        if (subtract != null && subtract.length > 0) {
            IPAddress subnetRight = subtract[0];
            //System.out.println("SubnetRight: " + subnetRight + " start from: " + subnetRight.getLower() + " to: " + subnetRight.getUpper());
            Optional<IPAddress> checkRight = subnetsToExclude.stream().filter(subnetRight::contains

            ).findFirst();
            if (checkRight.isPresent()) {
                exclude(subnetRight, subnetsToExclude, afterExclude);
            } else {
                afterExclude.add(subnetRight);
            }
        }
    }

}
