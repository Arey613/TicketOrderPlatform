export type EventPreview = {
  id: string;
  title: string;
  category: string;
  date: string;
  time: string;
  venue: string;
  city: string;
  price: string;
  availableSeats: number;
  accent: string;
};

export const eventPreviews: EventPreview[] = [
  {
    id: 'horizon-live',
    title: 'The Horizon Live',
    category: 'Rock concert',
    date: '24 May',
    time: '19:30',
    venue: 'Riverside Arena',
    city: 'Chisinau',
    price: '$59',
    availableSeats: 120,
    accent: 'from-teal-500 to-slate-900',
  },
  {
    id: 'city-hoops-finals',
    title: 'City Hoops Finals',
    category: 'Basketball',
    date: '31 May',
    time: '18:00',
    venue: 'Northside Stadium',
    city: 'Chisinau',
    price: '$35',
    availableSeats: 200,
    accent: 'from-red-500 to-zinc-900',
  },
  {
    id: 'laugh-out-loud',
    title: 'Laugh Out Loud',
    category: 'Stand-up comedy',
    date: '7 Jun',
    time: '20:00',
    venue: 'Harbour Theatre',
    city: 'Chisinau',
    price: '$45',
    availableSeats: 85,
    accent: 'from-amber-400 to-stone-900',
  },
];
