import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection, readResource, runMutation } from "@/lib/bookeat/services/data-source";
import type { CustomerWallet, RewardRules, WalletOverview, WalletTransaction } from "@/lib/bookeat/types";

function walletOverview(): WalletOverview {
  const issued = demoStore.walletTransactions.filter((item) => item.points > 0).reduce((sum, item) => sum + item.points, 0);
  const redeemed = Math.abs(demoStore.walletTransactions.filter((item) => item.type === "REDEEM").reduce((sum, item) => sum + item.points, 0));
  return {
    customersWithWallet: demoStore.wallets.length,
    totalPointsIssued: demoStore.wallets.reduce((sum, item) => sum + item.totalEarned, 0),
    totalPointsRedeemed: demoStore.wallets.reduce((sum, item) => sum + item.totalRedeemed, 0),
    outstandingPoints: demoStore.wallets.reduce((sum, item) => sum + item.balance, 0),
    todayEarnedPoints: issued,
    todayRedeemedPoints: redeemed,
  };
}

export const walletService = {
  getOverview: () => readResource(() => apiClient.get<WalletOverview>("/admin/wallet/overview"), walletOverview),
  listWallets: () => readCollection(() => apiClient.get<CustomerWallet[]>("/admin/wallet/customers"), () => demoStore.wallets),
  listTransactions: () => readCollection(() => apiClient.get<WalletTransaction[]>("/admin/wallet/transactions"), () => demoStore.walletTransactions),
  getRewardRules: () => readResource(() => apiClient.get<RewardRules>("/admin/wallet/reward-rules"), () => demoStore.rewardRules),
  updateRewardRules: (rules: RewardRules) => runMutation(
    () => apiClient.put<RewardRules>("/admin/wallet/reward-rules", rules),
    () => Object.assign(demoStore.rewardRules, rules),
  ),
  adjustPoints: (customerId: string, points: number, reason: string) => runMutation(
    () => apiClient.post<WalletTransaction>("/admin/wallet/adjustments", { customerId, points, reason }),
    () => {
      const wallet = demoStore.wallets.find((item) => item.customerId === customerId);
      if (!wallet) throw new Error("Customer wallet not found");
      wallet.balance += points;
      if (points > 0) wallet.totalEarned += points;
      else wallet.totalRedeemed += Math.abs(points);
      const transaction: WalletTransaction = {
        id: `WLT-${Date.now()}`,
        customerId,
        customerName: wallet.customerName,
        type: "ADJUSTMENT",
        points,
        reason,
        createdAt: "Just now",
        adminName: "Aarav Patel",
      };
      demoStore.walletTransactions.unshift(transaction);
      return transaction;
    },
  ),
};

