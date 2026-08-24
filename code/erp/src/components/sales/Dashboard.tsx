import React from 'react';
import { TrendingUp, ShoppingCart, DollarSign, Package } from 'lucide-react';

const StatCard = ({ title, value, change, icon: Icon, trend }: any) => (
  <div className="bg-white/80 backdrop-blur-md p-6 rounded-2xl border border-slate-100 shadow-sm hover:shadow-md transition-shadow">
    <div className="flex items-center justify-between mb-4">
      <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center">
        <Icon size={24} />
      </div>
      <span className={`text-sm font-medium px-2.5 py-1 rounded-full ${
        trend === 'up' ? 'text-emerald-700 bg-emerald-50' : 'text-red-700 bg-red-50'
      }`}>
        {change}
      </span>
    </div>
    <h3 className="text-slate-500 text-sm font-medium">{title}</h3>
    <p className="text-2xl font-bold text-slate-800 mt-1">{value}</p>
  </div>
);

export const Dashboard: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">Sales Dashboard</h1>
        <div className="flex gap-2">
          <select className="bg-white border border-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option>Last 7 Days</option>
            <option>This Month</option>
            <option>This Year</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
          title="Total Revenue" 
          value="$124,563.00" 
          change="+12.5%" 
          icon={DollarSign} 
          trend="up" 
        />
        <StatCard 
          title="Orders" 
          value="1,245" 
          change="+8.2%" 
          icon={ShoppingCart} 
          trend="up" 
        />
        <StatCard 
          title="Avg. Order Value" 
          value="$100.05" 
          change="-2.4%" 
          icon={TrendingUp} 
          trend="down" 
        />
        <StatCard 
          title="Items Sold" 
          value="4,890" 
          change="+14.1%" 
          icon={Package} 
          trend="up" 
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white/80 backdrop-blur-md rounded-2xl border border-slate-100 p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">Revenue Overview</h3>
          <div className="h-64 flex items-center justify-center border-2 border-dashed border-slate-200 rounded-xl bg-slate-50">
            <span className="text-slate-400">Chart Placeholder</span>
          </div>
        </div>
        <div className="bg-white/80 backdrop-blur-md rounded-2xl border border-slate-100 p-6 shadow-sm">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">Recent Activity</h3>
          <div className="space-y-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="flex items-center gap-4">
                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                <div>
                  <p className="text-sm font-medium text-slate-800">Order #ORD-{1000 + i} created</p>
                  <p className="text-xs text-slate-500">{i * 2} hours ago</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
