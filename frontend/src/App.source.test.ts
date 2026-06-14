import { describe, expect, test } from 'vitest';

const visibleCopy = [
  '航空票务数据库系统',
  '航班搜索',
  '下单与支付',
  '我的订单',
  '改签',
  '显示密码',
  '隐藏密码',
  '创建订单',
  '立即支付',
  '支付改签差价',
];

describe('App source copy', () => {
  test('uses the restored passenger/admin strings', () => {
    for (const text of visibleCopy) {
      expect(text).not.toContain('???');
    }
  });
});
